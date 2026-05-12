package de.t14d3.rapunzellib.nbt.shared;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

/**
 * Provides location context for entity NBT serialization/deserialization.
 * <p>
 * Implementations supply the server level and positional/rotational data
 * needed to reconstruct an entity from its serialized NBT.
 */
public interface SharedEntityLocation {
    /**
     * Returns the server level.
     *
     * @return the server level
     */
    @NotNull ServerLevel level();

    /**
     * Returns the X coordinate.
     *
     * @return the X coordinate
     */
    double x();

    /**
     * Returns the Y coordinate.
     *
     * @return the Y coordinate
     */
    double y();

    /**
     * Returns the Z coordinate.
     *
     * @return the Z coordinate
     */
    double z();

    /**
     * Returns the yaw rotation.
     *
     * @return the yaw
     */
    float yaw();

    /**
     * Returns the pitch rotation.
     *
     * @return the pitch
     */
    float pitch();
}
