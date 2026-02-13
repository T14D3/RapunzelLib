package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedBlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

final class FabricBlock extends SharedBlockBase {
    FabricBlock(ServerLevel world, BlockPos pos, FabricWorlds worlds) {
        super(
            PlatformId.FABRIC,
            Objects.requireNonNull(world, "world"),
            Objects.requireNonNull(pos, "pos"),
            de.t14d3.rapunzellib.attachments.RAttachmentContainer.lazyMutable(),
            Objects.requireNonNull(worlds, "worlds"),
            FabricBlockData::new
        );
    }
}
