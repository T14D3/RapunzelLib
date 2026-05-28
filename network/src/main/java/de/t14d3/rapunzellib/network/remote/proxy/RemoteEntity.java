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

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RemoteEntity extends RNativeBase implements REntity {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

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
        try {
            Requests.WorldRefResult result = gateway.callServer(serverName, EntityServiceMethods.GET_WORLD,
                new Requests.EntityRef(uuid), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (result != null && result.found() && result.worldRef() != null) {
                return Rapunzel.findContext().flatMap(ctx -> ctx.worlds().get(result.worldRef().key()));
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<RLocation> location() {
        try {
            Requests.LocationResult result = gateway.callServer(serverName, EntityServiceMethods.GET_LOCATION,
                new Requests.EntityRef(uuid), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (result != null && result.found() && result.world() != null) {
                return Optional.of(new RLocation(result.world(), result.x(), result.y(), result.z(), result.yaw(), result.pitch()));
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    @Override
    public boolean canTeleport() {
        return true;
    }

    @Override
    public boolean teleport(@NotNull RLocation location) {
        Objects.requireNonNull(location, "location");
        try {
            Requests.BooleanResult result = gateway.callServer(serverName, EntityServiceMethods.TELEPORT,
                new Requests.TeleportRequest(uuid, location.world(), location.x(), location.y(), location.z(), location.yaw(), location.pitch()),
                DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result != null && result.success();
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public @NotNull Optional<String> getName() {
        try {
            Requests.NameResult result = gateway.callServer(serverName, EntityServiceMethods.GET_NAME,
                new Requests.EntityRef(uuid), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result != null ? Optional.ofNullable(result.name()) : Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void setName(@NotNull String name) {
        Objects.requireNonNull(name, "name");
        try {
            gateway.callServer(serverName, EntityServiceMethods.SET_NAME,
                new Requests.SetNameRequest(uuid, name), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {}
    }

    @Override
    public @NotNull Optional<Component> getDisplayName() {
        try {
            Requests.ComponentResult result = gateway.callServer(serverName, EntityServiceMethods.GET_DISPLAY_NAME,
                new Requests.EntityRef(uuid), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (result != null && result.componentJson() != null && !result.componentJson().isJsonNull()) {
                return Optional.of(componentSerializer.deserialize(result.componentJson().getAsString()));
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    @Override
    public void setDisplayName(@NotNull Component displayName) {
        Objects.requireNonNull(displayName, "displayName");
        try {
            gateway.callServer(serverName, EntityServiceMethods.SET_DISPLAY_NAME,
                new Requests.SetDisplayNameRequest(uuid, componentSerializer.serializeToTree(displayName)), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean remove() {
        try {
            Requests.RemoveResult result = gateway.callServer(serverName, EntityServiceMethods.REMOVE,
                new Requests.EntityRef(uuid), DEFAULT_TIMEOUT)
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result != null && result.removed();
        } catch (Exception ignored) {
            return false;
        }
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
