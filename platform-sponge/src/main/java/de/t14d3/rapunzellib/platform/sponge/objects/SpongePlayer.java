package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class SpongePlayer extends RNativeHandle<ServerPlayer> implements RPlayer {
    SpongePlayer(ServerPlayer handle) {
        super(PlatformId.SPONGE, Objects.requireNonNull(handle, "handle"));
    }

    void updateHandle(ServerPlayer newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public Audience audience() {
        return handle();
    }

    @Override
    public UUID uuid() {
        return handle().profile().uuid();
    }

    @Override
    public String name() {
        return handle().profile().name().orElseGet(() -> handle().profile().uuid().toString());
    }

    @Override
    public boolean hasPermission(String permission) {
        if (permission == null || permission.isBlank()) return true;
        return handle().hasPermission(permission);
    }

    @Override
    public Optional<RWorld> world() {
        ServerWorld world = handle().world();
        return Rapunzel.context().worlds()
            .wrap(world)
            .or(() -> Optional.of(new SpongeWorld(world)));
    }

    @Override
    public Optional<RLocation> location() {
        ServerPlayer player = handle();
        Location<?, ?> location = player.location();
        if (!(location instanceof ServerLocation loc)) {
            return Optional.empty();
        }

        ServerWorld world = loc.world();
        String key = world.key().asString();
        String name = world.properties().name();

        Vector3d rot = player.rotation();
        float pitch = (float) rot.x();
        float yaw = (float) rot.y();

        return Optional.of(new RLocation(new RWorldRef(name, key), loc.x(), loc.y(), loc.z(), yaw, pitch));
    }

    @Override
    public boolean canTeleport() {
        return true;
    }

    @Override
    public void teleport(RLocation location) {
        Objects.requireNonNull(location, "location");
        if (!Sponge.isServerAvailable()) return;

        ServerPlayer player = handle();
        ServerWorld targetWorld = resolveWorld(location.world(), player.world());
        ServerLocation target = targetWorld.location(location.x(), location.y(), location.z());

        player.setLocation(target);

        Vector3d rot = new Vector3d(location.pitch(), location.yaw(), 0.0);
        player.setRotation(rot);
    }

    private static ServerWorld resolveWorld(RWorldRef worldRef, ServerWorld fallback) {
        if (worldRef == null) return fallback;
        var manager = Sponge.server().worldManager();

        String keyString = worldRef.key();
        if (keyString != null && !keyString.isBlank()) {
            try {
                ResourceKey key = ResourceKey.resolve(keyString.trim());
                Optional<ServerWorld> byKey = manager.world(key);
                if (byKey.isPresent()) return byKey.get();
            } catch (Exception ignored) {
            }
        }

        String name = worldRef.name();
        if (name != null && !name.isBlank()) {
            String target = name.trim();
            for (ServerWorld world : manager.worlds()) {
                if (world.properties().name().equalsIgnoreCase(target)) return world;
            }
        }

        return fallback;
    }
}
