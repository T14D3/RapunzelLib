package de.t14d3.rapunzellib.nbt.shared;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Provides location context for block entity NBT serialization/deserialization.
 * <p>
 * Implementations supply the block position, block state, and registry access
 * needed to reconstruct a block entity from its serialized NBT data.
 */
public interface SharedBlockEntityLocation {
    /**
     * Returns the block position.
     *
     * @return the block position
     */
    @NotNull BlockPos pos();

    /**
     * Returns the block state at this location.
     *
     * @return the block state
     */
    @NotNull BlockState state();

    /**
     * Returns the registry lookup provider.
     *
     * @return the registries
     */
    @NotNull HolderLookup.Provider registries();

    /**
     * Returns the X coordinate.
     *
     * @return the X coordinate
     */
    default int x() {
        return pos().getX();
    }

    /**
     * Returns the Y coordinate.
     *
     * @return the Y coordinate
     */
    default int y() {
        return pos().getY();
    }

    /**
     * Returns the Z coordinate.
     *
     * @return the Z coordinate
     */
    default int z() {
        return pos().getZ();
    }
}
