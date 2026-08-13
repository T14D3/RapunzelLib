package de.t14d3.rapunzellib.network.remote.handler;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.remote.rpc.PlayerServiceMethods;
import de.t14d3.rapunzellib.network.remote.rpc.Requests;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class PlayerRpcHandler {
    private static final Logger logger = LoggerFactory.getLogger(PlayerRpcHandler.class);
    private final GsonComponentSerializer componentSerializer = GsonComponentSerializer.gson();
    private volatile ServerConnector serverConnector;

    /**
     * Connects a player to another server. Receives the full
     * {@link Requests.ConnectToServerRequest} so the connector can act on the
     * destination location, not just the target server name.
     */
    @FunctionalInterface
    public interface ServerConnector {
        CompletableFuture<Boolean> connect(java.util.UUID playerUuid, Requests.ConnectToServerRequest request);
    }

    public void register(@NotNull NetworkRuntimeGateway gateway) {
        Objects.requireNonNull(gateway, "gateway");

        gateway.register(PlayerServiceMethods.GET_HEALTH, (req, source) ->
            withPlayer(req.uuid(), player ->
                new Requests.HealthResult(player.health(), player.maxHealth()))
        );

        gateway.register(PlayerServiceMethods.GET_AIR, (req, source) ->
            withPlayer(req.uuid(), player ->
                new Requests.AirResult(player.remainingAir(), player.maxAir()))
        );

        gateway.register(PlayerServiceMethods.IS_ALIVE, (req, source) ->
            withPlayer(req.uuid(), player ->
                new Requests.AliveResult(player.isAlive()))
        );

        gateway.register(PlayerServiceMethods.SEND_MESSAGE, (req, source) ->
            withPlayerVoid(req.uuid(), player -> {
                if (req.componentJson() != null && !req.componentJson().isJsonNull()) {
                    player.sendMessage(componentSerializer.deserialize(req.componentJson().getAsString()));
                }
                return new Requests.VoidResult();
            })
        );

        gateway.register(PlayerServiceMethods.SEND_ACTION_BAR, (req, source) ->
            withPlayerVoid(req.uuid(), player -> {
                if (req.componentJson() != null && !req.componentJson().isJsonNull()) {
                    player.sendActionBar(componentSerializer.deserialize(req.componentJson().getAsString()));
                }
                return new Requests.VoidResult();
            })
        );

        gateway.register(PlayerServiceMethods.HAS_PERMISSION, (req, source) ->
            withPlayer(req.uuid(), player ->
                new Requests.PermissionResult(player.hasPermission(req.permission())))
        );

        gateway.register(PlayerServiceMethods.DAMAGE, (req, source) ->
            withPlayer(req.uuid(), player -> {
                if (player instanceof RServerPlayer sp) {
                    return new Requests.BooleanResult(sp.damage(req.amount()));
                }
                return new Requests.BooleanResult(false);
            })
        );

        gateway.register(PlayerServiceMethods.HEAL, (req, source) ->
            withPlayer(req.uuid(), player -> {
                if (player instanceof RServerPlayer sp) {
                    return new Requests.BooleanResult(sp.heal(req.amount()));
                }
                return new Requests.BooleanResult(false);
            })
        );

        gateway.register(PlayerServiceMethods.CONNECT_TO_SERVER, (req, source) -> {
            if (req == null || req.uuid() == null || req.targetServer() == null) {
                return CompletableFuture.completedFuture(new Requests.BooleanResult(false));
            }
            if (serverConnector == null) {
                logger.warn("No ServerConnector registered for connectToServer; proxy support not installed");
                return CompletableFuture.completedFuture(new Requests.BooleanResult(false));
            }
            return serverConnector.connect(req.uuid(), req)
                .thenApply(success -> {
                    if (success && req.hasLocation() && req.world() != null) {
                        DeferredTeleportStore.store(req.uuid(), new RLocation(
                            req.world(), req.x(), req.y(), req.z(),
                            req.yaw(), req.pitch()));
                    }
                    return new Requests.BooleanResult(success);
                });
        });

        gateway.register(PlayerServiceMethods.GET_INVENTORY, (req, source) ->
            withPlayer(req.uuid(), player -> {
                try {
                    var inv = player.getClass().getMethod("inventory").invoke(player);
                    if (inv == null) {
                        return new Requests.InventorySnapshotResult("unknown", 0, List.of(), false);
                    }
                    int size = (int) inv.getClass().getMethod("size").invoke(inv);
                    List<Requests.SlotEntry> slots = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        Object item = inv.getClass().getMethod("item", int.class).invoke(inv, i);
                        if (item != null && item instanceof Optional<?> opt && opt.isPresent()) {
                            Object itemObj = opt.get();
                            String nbt = itemObj.getClass().getMethod("asString").invoke(itemObj).toString();
                            slots.add(new Requests.SlotEntry(i, nbt));
                        }
                    }
                    return new Requests.InventorySnapshotResult("inventory", size, slots, true);
                } catch (Exception e) {
                    logger.warn("Failed to capture inventory for {}: {}", req.uuid(), e.getMessage());
                    return new Requests.InventorySnapshotResult("unknown", 0, List.of(), false);
                }
            })
        );

        gateway.register(PlayerServiceMethods.SET_INVENTORY, (req, source) ->
            withPlayer(req.uuid(), player -> {
                try {
                    var inv = player.getClass().getMethod("inventory").invoke(player);
                    if (inv == null) return new Requests.BooleanResult(false);
                    int size = (int) inv.getClass().getMethod("size").invoke(inv);
                    inv.getClass().getMethod("clear").invoke(inv);
                    for (Requests.SlotEntry entry : req.slots()) {
                        if (entry.slot() >= 0 && entry.slot() < size && entry.itemNbt() != null) {
                            inv.getClass().getMethod("setItem", int.class, Optional.class)
                                .invoke(inv, entry.slot(), createItem(entry.itemNbt()));
                        }
                    }
                    return new Requests.BooleanResult(true);
                } catch (Exception e) {
                    logger.warn("Failed to set inventory for {}: {}", req.uuid(), e.getMessage());
                    return new Requests.BooleanResult(false);
                }
            })
        );

        // Server-authoritative presence: who is online on THIS server right now.
        // Lets cross-server tests prove a player really joined/left a backend.
        gateway.register(PlayerServiceMethods.QUERY_SERVER_PLAYERS, (req, source) -> {
            List<String> names = new ArrayList<>();
            for (RPlayer player : Rapunzel.players().online()) {
                if (player.asServerPlayer().isPresent()) {
                    names.add(player.name());
                }
            }
            names.sort(String::compareToIgnoreCase);
            return CompletableFuture.completedFuture(new Requests.ServerPlayersResult(names));
        });

        logger.info("[Remote] Registered player RPC handlers");
    }

    public void setServerConnector(@NotNull ServerConnector connector) {
        this.serverConnector = Objects.requireNonNull(connector, "connector");
    }

    private <T> @NotNull CompletableFuture<T> withPlayer(
        java.util.UUID uuid, java.util.function.Function<RServerPlayer, T> action
    ) {
        Optional<RPlayer> player = Rapunzel.players().get(uuid);
        if (player.isEmpty() || player.get().asServerPlayer().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return CompletableFuture.completedFuture(action.apply(player.get().requireServerPlayer()));
        } catch (Exception e) {
            logger.warn("Error handling player RPC for {}: {}", uuid, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private <T> @NotNull CompletableFuture<T> withPlayerVoid(
        java.util.UUID uuid, java.util.function.Function<RServerPlayer, T> action
    ) {
        return withPlayer(uuid, action);
    }

    private static Optional<?> createItem(String nbt) {
        try {
            Class<?> itemClass = Class.forName("de.t14d3.rapunzellib.nbt.item.RItem");
            java.lang.reflect.Method fromSnbt = itemClass.getMethod("fromSnbt", String.class);
            Object item = fromSnbt.invoke(null, nbt);
            return Optional.ofNullable(item);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
