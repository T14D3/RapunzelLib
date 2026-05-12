package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

/**
 * A precise location in a world, including rotation.
 *
 * @param world the world reference
 * @param x the x-coordinate
 * @param y the y-coordinate
 * @param z the z-coordinate
 * @param yaw the yaw rotation
 * @param pitch the pitch rotation
 */
public record RLocation(@NotNull RWorldRef world, double x, double y, double z, float yaw, float pitch) {
    /**
     * Converts this location to a block position by flooring coordinates.
     *
     * @return the block position
     */
    public @NotNull RBlockPos blockPos() {
        return new RBlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    /**
     * Creates a location with default yaw (0) and pitch (0).
     *
     * @param world the world reference
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param z the z-coordinate
     */
    public RLocation(@NotNull RWorldRef world, double x, double y, double z) {
        this(world, x, y, z, 0f, 0f);
    }
}

