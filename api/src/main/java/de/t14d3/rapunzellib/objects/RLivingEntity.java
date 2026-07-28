package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * A living entity with health, air, and damage/healing semantics.
 */
public interface RLivingEntity extends REntity {
    /** Returns the current health of this living entity. */
    double health();

    /** Returns the maximum health of this living entity. */
    double maxHealth();

    /** Returns the remaining air ticks for this living entity. */
    int remainingAir();

    /** Returns the maximum air ticks for this living entity. */
    int maxAir();

    /** Checks whether this living entity is alive. */
    boolean isAlive();

    /** Checks whether this living entity is dead. */
    default boolean isDead() {
        return !isAlive();
    }

    /** Returns whether raw generic damage semantics are available for this live wrapper. */
    default boolean canDamage() {
        return false;
    }

    /**
     * Applies raw generic damage to the live entity.
     *
     * @param amount the amount of damage to apply (must be non-negative)
     * @return true if the damage was applied, false otherwise
     */
    default boolean damage(double amount) {
        throw new UnsupportedOperationException("damage is not supported for " + getClass().getName());
    }

    /** Returns whether raw healing semantics are available for this live wrapper. */
    default boolean canHeal() {
        return false;
    }

    /**
     * Applies raw healing to the live entity.
     */
    default boolean heal(double amount) {
        throw new UnsupportedOperationException("heal is not supported for " + getClass().getName());
    }

    /** Looks up a living entity by UUID via the global entities access. */
    static @NotNull Optional<RLivingEntity> get(@NotNull UUID uuid) {
        return Rapunzel.entities().getLivingEntity(uuid);
    }

    /** Wraps a native platform living entity object into an RLivingEntity, if supported. */
    static @NotNull Optional<RLivingEntity> wrap(@NotNull Object nativeEntity) {
        return Rapunzel.entities().wrapLivingEntity(nativeEntity);
    }

    /** Wraps a native platform living entity object into an RLivingEntity, throwing if not possible. */
    static @NotNull RLivingEntity of(@NotNull Object nativeEntity) {
        return Rapunzel.entities().requireLivingEntity(nativeEntity);
    }
}
