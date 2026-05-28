package de.t14d3.rapunzellib.network.remote.proxy;

import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.network.remote.rpc.EntityServiceMethods;
import de.t14d3.rapunzellib.network.remote.rpc.PlayerServiceMethods;
import de.t14d3.rapunzellib.network.remote.rpc.Requests;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import de.t14d3.rapunzellib.registry.REntityType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RemotePlayer extends RemoteEntity implements RServerPlayer {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final String name;
    private final GsonComponentSerializer componentSerializer = GsonComponentSerializer.gson();

    public RemotePlayer(@NotNull UUID uuid, @NotNull String name,
                         @NotNull String serverName,
                         @NotNull NetworkRuntimeGateway gateway) {
        super(uuid, serverName, gateway, REntityType.ref("minecraft:player"), true);
        this.name = Objects.requireNonNull(name, "name");
    }

    public RemotePlayer(@NotNull UUID uuid, @NotNull String name,
                         @NotNull String serverName,
                         @NotNull NetworkRuntimeGateway gateway,
                         @NotNull RAttachmentContainer attachments) {
        super(uuid, serverName, gateway, REntityType.ref("minecraft:player"), true,
              Objects.requireNonNull(attachments, "attachments"));
        this.name = Objects.requireNonNull(name, "name");
    }

    public @NotNull CompletableFuture<Requests.LocationResult> locationAsync() {
        return gateway().callServer(serverName(), EntityServiceMethods.GET_LOCATION,
            new Requests.EntityRef(uuid()), DEFAULT_TIMEOUT);
    }

    public @NotNull CompletableFuture<Requests.BooleanResult> teleportAsync(@NotNull RLocation location) {
        Objects.requireNonNull(location, "location");
        return gateway().callServer(serverName(), EntityServiceMethods.TELEPORT,
            new Requests.TeleportRequest(uuid(), location.world(), location.x(), location.y(), location.z(), location.yaw(), location.pitch()),
            DEFAULT_TIMEOUT);
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission");
        try {
            Requests.PermissionResult result = gateway().callServer(serverName(), PlayerServiceMethods.HAS_PERMISSION,
                new Requests.PermissionRequest(uuid(), permission), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result != null && result.hasPermission();
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public double health() {
        try {
            Requests.HealthResult result = gateway().callServer(serverName(), PlayerServiceMethods.GET_HEALTH,
                new Requests.PlayerRef(uuid()), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result != null ? result.health() : 0.0;
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    @Override
    public double maxHealth() {
        try {
            Requests.HealthResult result = gateway().callServer(serverName(), PlayerServiceMethods.GET_HEALTH,
                new Requests.PlayerRef(uuid()), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result != null ? result.maxHealth() : 20.0;
        } catch (Exception ignored) {
            return 20.0;
        }
    }

    @Override
    public int remainingAir() {
        try {
            Requests.AirResult result = gateway().callServer(serverName(), PlayerServiceMethods.GET_AIR,
                new Requests.PlayerRef(uuid()), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result != null ? result.remainingAir() : 300;
        } catch (Exception ignored) {
            return 300;
        }
    }

    @Override
    public int maxAir() {
        try {
            Requests.AirResult result = gateway().callServer(serverName(), PlayerServiceMethods.GET_AIR,
                new Requests.PlayerRef(uuid()), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result != null ? result.maxAir() : 300;
        } catch (Exception ignored) {
            return 300;
        }
    }

    @Override
    public boolean isAlive() {
        try {
            Requests.AliveResult result = gateway().callServer(serverName(), PlayerServiceMethods.IS_ALIVE,
                new Requests.PlayerRef(uuid()), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result != null && result.alive();
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public boolean canDamage() {
        return true;
    }

    @Override
    public boolean damage(double amount) {
        try {
            Requests.BooleanResult result = gateway().callServer(serverName(), PlayerServiceMethods.DAMAGE,
                new Requests.DamageRequest(uuid(), amount), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result != null && result.success();
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public boolean canHeal() {
        return true;
    }

    @Override
    public boolean heal(double amount) {
        try {
            Requests.BooleanResult result = gateway().callServer(serverName(), PlayerServiceMethods.HEAL,
                new Requests.HealRequest(uuid(), amount), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result != null && result.success();
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public @NotNull Audience audience() {
        throw new UnsupportedOperationException("RemotePlayer has no local audience");
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        Objects.requireNonNull(message, "message");
        try {
            gateway().callServer(serverName(), PlayerServiceMethods.SEND_MESSAGE,
                new Requests.SendMessageRequest(uuid(), componentSerializer.serializeToTree(message)), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {}
    }

    @Override
    public void sendActionBar(@NotNull Component message) {
        Objects.requireNonNull(message, "message");
        try {
            gateway().callServer(serverName(), PlayerServiceMethods.SEND_ACTION_BAR,
                new Requests.SendMessageRequest(uuid(), componentSerializer.serializeToTree(message)), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {}
    }

    @Override
    public @NotNull Optional<RServerPlayer> asServerPlayer() {
        return Optional.of(this);
    }

    @Override
    public @NotNull RServerPlayer requireServerPlayer() {
        return this;
    }

    @Override
    public boolean isServerPlayer() {
        return true;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> connectToServer(@NotNull String targetServer, @NotNull RLocation location) {
        Objects.requireNonNull(targetServer, "targetServer");
        Objects.requireNonNull(location, "location");
        Requests.ConnectToServerRequest req = new Requests.ConnectToServerRequest(
            uuid(), targetServer, location.world(),
            location.x(), location.y(), location.z(),
            location.yaw(), location.pitch(), true);
        return gateway().callServer(serverName(), PlayerServiceMethods.CONNECT_TO_SERVER, req, DEFAULT_TIMEOUT)
            .thenApply(r -> r != null && r.success());
    }

    @Override
    public @NotNull CompletableFuture<Boolean> connectToServer(@NotNull String targetServer) {
        Objects.requireNonNull(targetServer, "targetServer");
        Requests.ConnectToServerRequest req = new Requests.ConnectToServerRequest(
            uuid(), targetServer, null, 0, 0, 0, 0, 0, false);
        return gateway().callServer(serverName(), PlayerServiceMethods.CONNECT_TO_SERVER, req, DEFAULT_TIMEOUT)
            .thenApply(r -> r != null && r.success());
    }

    public @NotNull CompletableFuture<Requests.InventorySnapshotResult> fetchInventoryAsync() {
        return gateway().callServer(serverName(), PlayerServiceMethods.GET_INVENTORY,
            new Requests.InventorySnapshotRequest(uuid()), DEFAULT_TIMEOUT);
    }

    public @NotNull CompletableFuture<Boolean> setInventoryAsync(@NotNull List<Requests.SlotEntry> slots,
                                                                    @NotNull String inventoryType) {
        return gateway().callServer(serverName(), PlayerServiceMethods.SET_INVENTORY,
            new Requests.SetInventoryRequest(uuid(), inventoryType, slots), DEFAULT_TIMEOUT)
            .thenApply(r -> r != null && r.success());
    }
}