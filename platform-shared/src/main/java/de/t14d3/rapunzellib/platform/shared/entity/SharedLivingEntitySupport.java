package de.t14d3.rapunzellib.platform.shared.entity;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/** Static utility providing shared living entity property access. */
public final class SharedLivingEntitySupport {
    private SharedLivingEntitySupport() {
    }

    public static double health(@NotNull LivingEntity entity) {
        return entity.getHealth();
    }

    public static double maxHealth(@NotNull LivingEntity entity) {
        return entity.getMaxHealth();
    }

    public static int remainingAir(@NotNull LivingEntity entity) {
        return entity.getAirSupply();
    }

    public static int maxAir(@NotNull LivingEntity entity) {
        return entity.getMaxAirSupply();
    }

    public static boolean isAlive(@NotNull LivingEntity entity) {
        return entity.isAlive();
    }
}
