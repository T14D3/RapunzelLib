package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public interface RLivingEntity extends REntity {
    double health();

    double maxHealth();

    int remainingAir();

    int maxAir();

    boolean isAlive();

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

    static @NotNull Optional<RLivingEntity> get(@NotNull UUID uuid) {
        return Rapunzel.entities().getLivingEntity(uuid);
    }

    static @NotNull Optional<RLivingEntity> wrap(@NotNull Object nativeEntity) {
        return Rapunzel.entities().wrapLivingEntity(nativeEntity);
    }
}
