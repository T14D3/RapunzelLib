package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.entity.SharedBlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class FabricBlock extends SharedBlockBase {
    FabricBlock(ServerLevel world, BlockPos pos, @NotNull SharedAttachmentService attachmentService, FabricWorlds worlds) {
        super(
            PlatformId.FABRIC,
            Objects.requireNonNull(world, "world"),
            Objects.requireNonNull(pos, "pos"),
            attachmentService.forBlock(world, pos),
            Objects.requireNonNull(worlds, "worlds"),
            FabricBlockData::new
        );
    }
}
