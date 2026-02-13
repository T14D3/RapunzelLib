package de.t14d3.rapunzellib.nbt.fabric;

import de.t14d3.rapunzellib.nbt.shared.SharedEntityLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a location in a Fabric/Minecraft world.
 * Used for entity deserialization.
 *
 * @param level the server level
 * @param x the x coordinate
 * @param y the y coordinate
 * @param z the z coordinate
 * @param yaw the yaw rotation
 * @param pitch the pitch rotation
 */
public record FabricLocation(
        @NotNull ServerLevel level,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) implements SharedEntityLocation {
    /**
     * Creates a location without rotation.
     *
     * @param level the server level
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public FabricLocation(@NotNull ServerLevel level, double x, double y, double z) {
        this(level, x, y, z, 0.0f, 0.0f);
    }

    /**
     * Creates a location from a Vec3 position.
     *
     * @param level the server level
     * @param pos the Vec3 position
     */
    public FabricLocation(@NotNull ServerLevel level, net.minecraft.world.phys.Vec3 pos) {
        this(level, pos.x, pos.y, pos.z, 0.0f, 0.0f);
    }
}
