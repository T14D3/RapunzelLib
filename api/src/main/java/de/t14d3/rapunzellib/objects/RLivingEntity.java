package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * A living entity with health, air, and damage/healing semantics.
 */
public interface RLivingEntity extends REntity {
    /**
     * Returns the current health of this living entity.
     *
     * @return the health value
     */
    double health();

    /**
     * Returns the maximum health of this living entity.
     *
     * @return the maximum health value
     */
    double maxHealth();

    /**
     * Returns the remaining air ticks for this living entity.
     *
     * @return the remaining air
     */
    int remainingAir();

    /**
     * Returns the maximum air ticks for this living entity.
     *
     * @return the maximum air
     */
    int maxAir();

    /**
     * Checks whether this living entity is alive.
     *
     * @return true if alive
     */
    boolean isAlive();

    /**
     * Checks whether this living entity is dead.
     *
     * @return true if dead
     */
    default boolean isDead() {
        return !isAlive();
    }

    /**
     * Returns whether raw generic damage semantics are available for this live wrapper.
     */
    default boolean canDamage() {
        return false;
    }

    /**
     * Applies raw generic damage to the live entity.
     */
    default boolean damage(double amount) {
        throw new UnsupportedOperationException("damage is not supported for " + getClass().getName());
    }

    /**
     * Returns whether raw healing semantics are available for this live wrapper.
     */
    default boolean canHeal() {
        return false;
    }

    /**
     * Applies raw healing to the live entity.
     */
    default boolean heal(double amount) {
        throw new UnsupportedOperationException("heal is not supported for " + getClass().getName());
    }

    /**
     * Looks up a living entity by UUID via the global entities access.
     *
     * @param uuid the entity UUID
     * @return an {@link Optional} containing the living entity, or empty if not found
     */
    static @NotNull Optional<RLivingEntity> get(@NotNull UUID uuid) {
        return Rapunzel.entities().getLivingEntity(uuid);
    }

    /**
     * Wraps a native platform living entity object into an RLivingEntity, if supported.
     *
     * @param nativeEntity the native living entity object
     * @return an {@link Optional} containing the wrapped living entity, or empty if wrapping is not supported
     */
    static @NotNull Optional<RLivingEntity> wrap(@NotNull Object nativeEntity) {
        return Rapunzel.entities().wrapLivingEntity(nativeEntity);
    }
}
