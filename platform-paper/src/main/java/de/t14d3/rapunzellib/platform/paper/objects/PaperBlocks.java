package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.platform.shared.entity.SharedBlocksCore;
import de.t14d3.rapunzellib.platform.paper.PaperHandleBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class PaperBlocks extends SharedBlocksCore<PaperBlock, PaperBlockData> {
    private final PaperWorlds worlds;

    public PaperBlocks(@NotNull PaperWorlds worlds) {
        this.worlds = worlds;
    }

    @Override
    public @NotNull Optional<RBlockData> wrapData(@NotNull Object nativeBlockData) {
        if (nativeBlockData instanceof BlockData data) {
            return java.util.Optional.of(new PaperBlockData(PaperHandleBridge.toNms(data)));
        }
        if (nativeBlockData instanceof Block block) {
            return java.util.Optional.of(new PaperBlockData(PaperHandleBridge.toNms(block.getBlockData())));
        }
        return super.wrapData(nativeBlockData);
    }

    @Override
    protected @NotNull PaperBlock createBlock(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        return new PaperBlock(level, pos, worlds);
    }

    @Override
    protected @NotNull PaperBlockData createBlockData(@NotNull BlockState state) {
        return new PaperBlockData(state);
    }

    @Override
    protected @NotNull java.util.Optional<NativeBlockRef> adaptNativeBlock(@NotNull Object nativeBlock) {
        if (nativeBlock instanceof Block block) {
            return java.util.Optional.of(new NativeBlockRef(
                PaperHandleBridge.toNms(block.getWorld()),
                new BlockPos(block.getX(), block.getY(), block.getZ())
            ));
        }
        return super.adaptNativeBlock(nativeBlock);
    }
}
