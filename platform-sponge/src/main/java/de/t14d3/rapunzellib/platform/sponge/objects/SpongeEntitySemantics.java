package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import java.util.Objects;
import java.util.Optional;

final class SpongeEntitySemantics {
    private SpongeEntitySemantics() {
    }

    static @NotNull RWorldRef worldRef(@NotNull ServerWorld world) {
        Objects.requireNonNull(world, "world");
        return new RWorldRef(world.properties().name(), RKey.of(world.key().asString()));
    }

    static @NotNull RLocation location(@NotNull Entity entity) {
        Objects.requireNonNull(entity, "entity");
        ServerLocation location = entity.serverLocation();
        Vector3d rotation = entity.rotation();
        return new RLocation(
            worldRef(location.world()),
            location.x(),
            location.y(),
            location.z(),
            (float) rotation.y(),
            (float) rotation.x()
        );
    }

    static boolean teleport(@NotNull Entity entity, @NotNull RLocation location) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(location, "location");
        if (!Sponge.isServerAvailable()) {
            return false;
        }

        ServerWorld targetWorld = resolveWorld(location.world(), entity.serverLocation().world());
        boolean moved = entity.setLocation(targetWorld.location(location.x(), location.y(), location.z()));
        if (moved) {
            applyRotation(entity, location);
        }
        return moved;
    }

    static void applyRotation(@NotNull Entity entity, @NotNull RLocation location) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(location, "location");
        entity.setRotation(new Vector3d(location.pitch(), location.yaw(), 0.0d));
    }

    static @NotNull ServerWorld resolveWorld(RWorldRef worldRef, @NotNull ServerWorld fallback) {
        Objects.requireNonNull(fallback, "fallback");
        if (worldRef == null) {
            return fallback;
        }

        var manager = Sponge.server().worldManager();
        RKey key = worldRef.key();
        if (key != null) {
            try {
                Optional<ServerWorld> byKey = manager.world(ResourceKey.resolve(key.asString()));
                if (byKey.isPresent()) {
                    return byKey.get();
                }
            } catch (Exception ignored) {
            }
        }

        String name = worldRef.name();
        if (name != null && !name.isBlank()) {
            String target = name.trim();
            for (ServerWorld world : manager.worlds()) {
                if (world.properties().name().equalsIgnoreCase(target)) {
                    return world;
                }
            }
        }
        return fallback;
    }

    static boolean damage(@NotNull Living living, double amount) {
        requireFiniteNonNegative(amount, "amount");
        if (amount == 0.0d) {
            return true;
        }

        double before = living.health().get();
        living.damage(amount, DamageSource.builder().type(DamageTypes.GENERIC).build());
        return living.health().get() < before || living.isRemoved();
    }

    static boolean heal(@NotNull Living living, double amount) {
        requireFiniteNonNegative(amount, "amount");
        if (amount == 0.0d) {
            return true;
        }

        double next = Math.min(living.maxHealth().get(), living.health().get() + amount);
        living.offer(Keys.HEALTH, next);
        return true;
    }

    private static void requireFiniteNonNegative(double amount, @NotNull String name) {
        if (!Double.isFinite(amount) || amount < 0.0d) {
            throw new IllegalArgumentException(name + " must be a finite non-negative number");
        }
    }
}
