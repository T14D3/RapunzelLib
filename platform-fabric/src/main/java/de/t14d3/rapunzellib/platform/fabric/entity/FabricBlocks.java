package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.entity.SharedBlocksCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class FabricBlocks extends SharedBlocksCore<FabricBlock, FabricBlockData> {
    private final SharedAttachmentService attachmentService;
    private final FabricWorlds worlds;

    public FabricBlocks(@NotNull SharedAttachmentService attachmentService, @NotNull FabricWorlds worlds) {
        this.attachmentService = attachmentService;
        this.worlds = worlds;
    }

    @Override
    protected @NotNull FabricBlock createBlock(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        return new FabricBlock(level, pos, attachmentService, worlds);
    }

    @Override
    protected @NotNull FabricBlockData createBlockData(@NotNull BlockState state) {
        return new FabricBlockData(state);
    }
}
