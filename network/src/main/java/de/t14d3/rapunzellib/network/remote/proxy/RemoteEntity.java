package de.t14d3.rapunzellib.network.remote.proxy;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.network.remote.rpc.EntityServiceMethods;
import de.t14d3.rapunzellib.network.remote.rpc.Requests;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.objects.*;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RemoteEntity extends RNativeBase implements REntity {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private static final Logger logger = LoggerFactory.getLogger(RemoteEntity.class);

    private final UUID uuid;
    private final String serverName;
    private final NetworkRuntimeGateway gateway;
    private final RRegistryRef<REntityType> typeRef;
    private final boolean isLiving;
    private final GsonComponentSerializer componentSerializer = GsonComponentSerializer.gson();

    public RemoteEntity(@NotNull UUID uuid, @NotNull String serverName,
                         @NotNull NetworkRuntimeGateway gateway,
                         @NotNull RRegistryRef<REntityType> typeRef,
                         boolean isLiving) {
        super(PlatformId.REMOTE);
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.typeRef = Objects.requireNonNull(typeRef, "typeRef");
        this.isLiving = isLiving;
    }

    public RemoteEntity(@NotNull UUID uuid, @NotNull String serverName,
                         @NotNull NetworkRuntimeGateway gateway,
                         @NotNull RRegistryRef<REntityType> typeRef,
                         boolean isLiving,
                         @NotNull RAttachmentContainer attachments) {
        super(PlatformId.REMOTE, Objects.requireNonNull(attachments, "attachments"));
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.typeRef = Objects.requireNonNull(typeRef, "typeRef");
        this.isLiving = isLiving;
    }

    /**
     * Blocks on the given RPC call for the default timeout and returns its result,
     * or {@code null} if the call failed. An interrupted caller thread keeps its
     * interrupt flag (the interruption is logged at debug level); all other failures
     * (timeouts, execution errors, closed gateway) are logged at warn level. The call
     * is not retried and no exception escapes, so callers distinguish "absent value"
     * ({@code null} result) from a hard failure only via the log.
     */
    protected final <T> T awaitRpc(@NotNull String operation, @NotNull Supplier<CompletableFuture<T>> call) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(call, "call");
        try {
            CompletableFuture<T> future = call.get();
            return future.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("RPC '{}' interrupted (remote {}, server {})", operation, uuid, serverName, e);
            return null;
        } catch (Exception e) {
            logger.warn("RPC '{}' failed (remote {}, server {})", operation, uuid, serverName, e);
            return null;
        }
    }

    public @NotNull String serverName() {
        return serverName;
    }

    public @NotNull NetworkRuntimeGateway gateway() {
        return gateway;
    }

    @Override
    public @NotNull UUID uuid() {
        return uuid;
    }

    @Override
    public @NotNull RRegistryRef<REntityType> typeRef() {
        return typeRef;
    }

    @Override
    public @NotNull Object handle() {
        return uuid.toString();
    }

    @Override
    public @NotNull Optional<RWorld> world() {
        Requests.WorldRefResult result = awaitRpc("world",
            () -> gateway.callServer(serverName, EntityServiceMethods.GET_WORLD,
                new Requests.EntityRef(uuid), DEFAULT_TIMEOUT));
        if (result != null && result.found() && result.worldRef() != null) {
            return Rapunzel.findContext().flatMap(ctx -> ctx.worlds().get(result.worldRef().key()));
        }
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<RLocation> location() {
        Requests.LocationResult result = awaitRpc("location",
            () -> gateway.callServer(serverName, EntityServiceMethods.GET_LOCATION,
                new Requests.EntityRef(uuid), DEFAULT_TIMEOUT));
        if (result != null && result.found() && result.world() != null) {
            return Optional.of(new RLocation(result.world(), result.x(), result.y(), result.z(), result.yaw(), result.pitch()));
        }
        return Optional.empty();
    }

    @Override
    public boolean canTeleport() {
        return true;
    }

    @Override
    public boolean teleport(@NotNull RLocation location) {
        Objects.requireNonNull(location, "location");
        Requests.BooleanResult result = awaitRpc("teleport",
            () -> gateway.callServer(serverName, EntityServiceMethods.TELEPORT,
                new Requests.TeleportRequest(uuid, location.world(), location.x(), location.y(), location.z(), location.yaw(), location.pitch()),
                DEFAULT_TIMEOUT));
        return result != null && result.success();
    }

    @Override
    public @NotNull Optional<String> getName() {
        Requests.NameResult result = awaitRpc("getName",
            () -> gateway.callServer(serverName, EntityServiceMethods.GET_NAME,
                new Requests.EntityRef(uuid), DEFAULT_TIMEOUT));
        return result != null ? Optional.ofNullable(result.name()) : Optional.empty();
    }

    @Override
    public void setName(@NotNull String name) {
        Objects.requireNonNull(name, "name");
        awaitRpc("setName",
            () -> gateway.callServer(serverName, EntityServiceMethods.SET_NAME,
                new Requests.SetNameRequest(uuid, name), DEFAULT_TIMEOUT));
    }

    @Override
    public @NotNull Optional<Component> getDisplayName() {
        Requests.ComponentResult result = awaitRpc("getDisplayName",
            () -> gateway.callServer(serverName, EntityServiceMethods.GET_DISPLAY_NAME,
                new Requests.EntityRef(uuid), DEFAULT_TIMEOUT));
        if (result != null && result.componentJson() != null && !result.componentJson().isJsonNull()) {
            return Optional.of(componentSerializer.deserialize(result.componentJson().getAsString()));
        }
        return Optional.empty();
    }

    @Override
    public void setDisplayName(@NotNull Component displayName) {
        Objects.requireNonNull(displayName, "displayName");
        awaitRpc("setDisplayName",
            () -> gateway.callServer(serverName, EntityServiceMethods.SET_DISPLAY_NAME,
                new Requests.SetDisplayNameRequest(uuid, componentSerializer.serializeToTree(displayName)), DEFAULT_TIMEOUT));
    }

    @Override
    public boolean remove() {
        Requests.RemoveResult result = awaitRpc("remove",
            () -> gateway.callServer(serverName, EntityServiceMethods.REMOVE,
                new Requests.EntityRef(uuid), DEFAULT_TIMEOUT));
        return result != null && result.removed();
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    @Override
    public boolean isLivingEntity() {
        return isLiving;
    }
}
