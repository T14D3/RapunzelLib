package de.t14d3.rapunzellib.nbt.sponge;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.world.World;
import org.spongepowered.math.vector.Vector3d;

/**
 * Represents a location in a Sponge world.
 * Used for entity deserialization.
 *
 * @param world the Sponge world
 * @param position the position vector
 * @param yaw the yaw rotation
 * @param pitch the pitch rotation
 */
public record SpongeLocation(
    @NotNull World<?, ?> world,
    @NotNull Vector3d position,
    double yaw,
    double pitch
) {
    /**
     * Creates a location without rotation.
     *
     * @param world the Sponge world
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public SpongeLocation(@NotNull World<?, ?> world, double x, double y, double z) {
        this(world, new Vector3d(x, y, z), 0.0, 0.0);
    }
    
    /**
     * Creates a location without rotation.
     *
     * @param world the Sponge world
     * @param position the position vector
     */
    public SpongeLocation(@NotNull World<?, ?> world, @NotNull Vector3d position) {
        this(world, position, 0.0, 0.0);
    }
    
    /**
     * Gets the x coordinate.
     *
     * @return the x coordinate
     */
    public double x() {
        return position.x();
    }
    
    /**
     * Gets the y coordinate.
     *
     * @return the y coordinate
     */
    public double y() {
        return position.y();
    }
    
    /**
     * Gets the z coordinate.
     *
     * @return the z coordinate
     */
    public double z() {
        return position.z();
    }
}
