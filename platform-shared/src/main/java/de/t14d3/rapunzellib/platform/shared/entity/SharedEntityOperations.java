package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryHandles;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

/**
 * Static utility providing shared cross-entity operations such as teleportation,
 * damage, healing, and entity spawning.
 * <p>
 * All methods validate their arguments and enforce finite coordinate semantics
 * via {@link SharedLocationSemantics}.
 * </p>
 */
public final class SharedEntityOperations {
    private SharedEntityOperations() {
    }

    /**
     * Teleports a native entity to the specified location, potentially crossing world boundaries.
     *
     * @param entity     the native entity to teleport
     * @param location   the target location
     * @param worldHooks hooks for world resolution and creation
     * @return {@code true} if the teleport was successful, {@code false} otherwise
     */
    public static boolean teleport(@NotNull Entity entity, @NotNull RLocation location, @NotNull SharedWorldHooks worldHooks) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(worldHooks, "worldHooks");
        SharedLocationSemantics.requireFinite(location);

        ServerLevel currentLevel = (ServerLevel) entity.level();
        MinecraftServer server = currentLevel.getServer();
        ServerLevel targetLevel = worldHooks.resolveWorld(server, location.world()).orElse(currentLevel);
        EnumSet<Relative> relatives = EnumSet.noneOf(Relative.class);

        boolean moved = entity.teleportTo(targetLevel, location.x(), location.y(), location.z(), relatives, location.yaw(), location.pitch(), true);
        if (moved) {
            SharedLocationSemantics.apply(entity, location);
        }
        return moved;
    }

    /**
     * Applies damage to a living entity.
     *
     * @param entity the living entity to damage
     * @param amount the amount of damage to apply (must be finite and non-negative)
     * @return {@code true} if the damage was applied (or amount was zero), {@code false} otherwise
     * @throws IllegalArgumentException if amount is not finite or is negative
     */
    public static boolean damage(@NotNull LivingEntity entity, double amount) {
        Objects.requireNonNull(entity, "entity");
        requireFiniteNonNegative(amount, "amount");
        if (amount == 0.0d) {
            return true;
        }
        return entity.hurtServer((ServerLevel) entity.level(), entity.damageSources().generic(), (float) amount);
    }

    /**
     * Heals a living entity by the specified amount.
     *
     * @param entity the living entity to heal
     * @param amount the amount of health to restore (must be finite and non-negative)
     * @return {@code true} always (healing is non-fatal)
     * @throws IllegalArgumentException if amount is not finite or is negative
     */
    public static boolean heal(@NotNull LivingEntity entity, double amount) {
        Objects.requireNonNull(entity, "entity");
        requireFiniteNonNegative(amount, "amount");
        if (amount == 0.0d) {
            return true;
        }
        entity.heal((float) amount);
        return true;
    }

    /**
     * Spawns an entity of the given type at the specified location.
     *
     * @param level      the server level to spawn in
     * @param type       the registry reference for the entity type
     * @param location   the spawn location (coordinates and rotation)
     * @param worldHooks hooks for world resolution
     * @return an Optional containing the spawned entity wrapper, or empty if spawning failed
     */
    public static @NotNull Optional<REntity> spawn(
        @NotNull ServerLevel level,
        @NotNull RRegistryRef<REntityType> type,
        @NotNull RLocation location,
        @NotNull SharedWorldHooks worldHooks
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(worldHooks, "worldHooks");
        SharedLocationSemantics.requireFinite(location);

        MinecraftServer server = level.getServer();
        ServerLevel targetLevel = worldHooks.resolveWorld(server, location.world()).orElse(level);

        EntityType<?> entityType = RRegistryHandles.find(type, EntityType.class).orElse(null);
        if (entityType == null) {
            return Optional.empty();
        }

        Entity entity = entityType.create(targetLevel, EntitySpawnReason.COMMAND);
        if (entity == null) {
            return Optional.empty();
        }

        SharedLocationSemantics.apply(entity, location);
        if (!targetLevel.addFreshEntity(entity)) {
            return Optional.empty();
        }
        return Rapunzel.context().entities().wrap(entity);
    }

    /**
     * Validates that the given amount is finite and non-negative.
     *
     * @param amount the value to check
     * @param name   the parameter name for the error message
     * @throws IllegalArgumentException if the amount is not finite or is negative
     */
    private static void requireFiniteNonNegative(double amount, @NotNull String name) {
        if (!Double.isFinite(amount) || amount < 0.0d) {
            throw new IllegalArgumentException(name + " must be a finite non-negative number");
        }
    }
}
