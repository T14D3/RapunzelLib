package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.platform.shared.entity.SharedBlocksCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeBlocks extends SharedBlocksCore<NeoForgeBlock, NeoForgeBlockData> {
    private final NeoForgeWorlds worlds;

    public NeoForgeBlocks(@NotNull NeoForgeWorlds worlds) {
        this.worlds = worlds;
    }

    @Override
    protected @NotNull NeoForgeBlock createBlock(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        return new NeoForgeBlock(level, pos, worlds);
    }

    @Override
    protected @NotNull NeoForgeBlockData createBlockData(@NotNull BlockState state) {
        return new NeoForgeBlockData(state);
    }
}
