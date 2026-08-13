package de.t14d3.rapunzellib.network.remote.handler;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.remote.rpc.EntityServiceMethods;
import de.t14d3.rapunzellib.network.remote.rpc.Requests;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.objects.*;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class EntityRpcHandler {
    private static final Logger logger = LoggerFactory.getLogger(EntityRpcHandler.class);
    private final GsonComponentSerializer componentSerializer = GsonComponentSerializer.gson();

    public void register(@NotNull NetworkRuntimeGateway gateway) {
        Objects.requireNonNull(gateway, "gateway");

        gateway.register(EntityServiceMethods.TELEPORT, (req, source) ->
            withEntity(req.uuid(), entity -> {
                RLocation loc = new RLocation(req.world(), req.x(), req.y(), req.z(), req.yaw(), req.pitch());
                return new Requests.BooleanResult(entity.teleport(loc));
            })
        );

        gateway.register(EntityServiceMethods.GET_LOCATION, (req, source) ->
            withEntity(req.uuid(), entity -> {
                Optional<RLocation> loc = entity.location();
                if (loc.isPresent()) {
                    RLocation l = loc.get();
                    return new Requests.LocationResult(l.world(), l.x(), l.y(), l.z(), l.yaw(), l.pitch(), true);
                }
                return new Requests.LocationResult(null, 0, 0, 0, 0, 0, false);
            })
        );

        gateway.register(EntityServiceMethods.GET_WORLD, (req, source) ->
            withEntity(req.uuid(), entity -> {
                Optional<RWorldRef> ref = entity.worldRef();
                return new Requests.WorldRefResult(ref.orElse(null), ref.isPresent());
            })
        );

        gateway.register(EntityServiceMethods.GET_NAME, (req, source) ->
            withEntity(req.uuid(), entity ->
                new Requests.NameResult(entity.getName().orElse(null)))
        );

        gateway.register(EntityServiceMethods.SET_NAME, (req, source) ->
            withEntityVoid(req.uuid(), entity -> {
                entity.setName(req.name());
                return new Requests.VoidResult();
            })
        );

        gateway.register(EntityServiceMethods.GET_DISPLAY_NAME, (req, source) ->
            withEntity(req.uuid(), entity -> {
                var display = entity.getDisplayName();
                if (display.isPresent()) {
                    return new Requests.ComponentResult(componentSerializer.serializeToTree(display.get()));
                }
                return new Requests.ComponentResult(null);
            })
        );

        gateway.register(EntityServiceMethods.SET_DISPLAY_NAME, (req, source) ->
            withEntityVoid(req.uuid(), entity -> {
                if (req.componentJson() != null && !req.componentJson().isJsonNull()) {
                    entity.setDisplayName(componentSerializer.deserialize(req.componentJson().getAsString()));
                }
                return new Requests.VoidResult();
            })
        );

        gateway.register(EntityServiceMethods.REMOVE, (req, source) ->
            withEntity(req.uuid(), entity ->
                new Requests.RemoveResult(entity.remove()))
        );

        // Server-authoritative entity presence: does the given entity exist on
        // THIS server right now? Lets cross-server tests prove an entity really
        // arrived on (or left) a backend after a transfer.
        gateway.register(EntityServiceMethods.QUERY_ENTITY_PRESENCE, (req, source) -> {
            if (req == null || req.uuid() == null) {
                return CompletableFuture.completedFuture(new Requests.EntityPresenceResult(false, null, null));
            }
            Optional<REntity> entity = Rapunzel.entities().get(req.uuid());
            if (entity.isEmpty()) {
                return CompletableFuture.completedFuture(new Requests.EntityPresenceResult(false, null, null));
            }
            REntity found = entity.get();
            String type = found.typeKey().asString();
            String world = found.worldRef().map(RWorldRef::identifier).orElse(null);
            logger.debug("[Remote] Entity presence {} found={} type={} world={}",
                req.uuid(), true, type, world);
            return CompletableFuture.completedFuture(new Requests.EntityPresenceResult(true, type, world));
        });

        logger.info("[Remote] Registered entity RPC handlers");
    }

    private <T> @NotNull CompletableFuture<T> withEntity(
        java.util.UUID uuid, java.util.function.Function<REntity, T> action
    ) {
        Optional<REntity> entity = Rapunzel.entities().get(uuid);
        if (entity.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return CompletableFuture.completedFuture(action.apply(entity.get()));
        } catch (Exception e) {
            logger.warn("Error handling entity RPC for {}: {}", uuid, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private <T> @NotNull CompletableFuture<T> withEntityVoid(
        java.util.UUID uuid, java.util.function.Function<REntity, T> action
    ) {
        return withEntity(uuid, action);
    }
}
