package de.t14d3.rapunzellib.nbt.shared;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface SharedBlockEntityLocation {
    @NotNull BlockPos pos();

    @NotNull BlockState state();

    @NotNull HolderLookup.Provider registries();

    default int x() {
        return pos().getX();
    }

    default int y() {
        return pos().getY();
    }

    default int z() {
        return pos().getZ();
    }
}
