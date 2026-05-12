package de.t14d3.rapunzellib.platform.shared.entity;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Static utility providing shared living entity property access.
 * <p>
 * Wraps common {@link LivingEntity} getters for health, air supply, and
 * alive state, used by both {@link SharedLivingEntityBase} and {@link SharedServerPlayerBase}.
 * </p>
 */
public final class SharedLivingEntitySupport {
    private SharedLivingEntitySupport() {
    }

    /**
     * Gets the current health of the living entity.
     *
     * @param entity the living entity
     * @return the current health value
     */
    public static double health(@NotNull LivingEntity entity) {
        return entity.getHealth();
    }

    /**
     * Gets the maximum health of the living entity.
     *
     * @param entity the living entity
     * @return the maximum health value
     */
    public static double maxHealth(@NotNull LivingEntity entity) {
        return entity.getMaxHealth();
    }

    /**
     * Gets the remaining air supply of the living entity.
     *
     * @param entity the living entity
     * @return the remaining air ticks
     */
    public static int remainingAir(@NotNull LivingEntity entity) {
        return entity.getAirSupply();
    }

    /**
     * Gets the maximum air supply of the living entity.
     *
     * @param entity the living entity
     * @return the maximum air ticks
     */
    public static int maxAir(@NotNull LivingEntity entity) {
        return entity.getMaxAirSupply();
    }

    /**
     * Checks whether the living entity is alive.
     *
     * @param entity the living entity
     * @return {@code true} if the entity is alive
     */
    public static boolean isAlive(@NotNull LivingEntity entity) {
        return entity.isAlive();
    }
}
