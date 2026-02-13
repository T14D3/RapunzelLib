package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedBlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

final class NeoForgeBlock extends SharedBlockBase {
    NeoForgeBlock(ServerLevel world, BlockPos pos, NeoForgeWorlds worlds) {
        super(
            PlatformId.NEOFORGE,
            Objects.requireNonNull(world, "world"),
            Objects.requireNonNull(pos, "pos"),
            de.t14d3.rapunzellib.attachments.RAttachmentContainer.lazyMutable(),
            Objects.requireNonNull(worlds, "worlds"),
            NeoForgeBlockData::new
        );
    }
}
