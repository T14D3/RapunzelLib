package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedBlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.UUID;

final class PaperBlock extends SharedBlockBase {
    PaperBlock(ServerLevel level, BlockPos pos, PaperWorlds worlds) {
        super(
            PlatformId.PAPER,
            Objects.requireNonNull(level, "level"),
            Objects.requireNonNull(pos, "pos"),
            PaperPersistentAttachments.forBlock(worlds.cachedWorldUuid(level), pos.getX(), pos.getY(), pos.getZ()),
            Objects.requireNonNull(worlds, "worlds"),
            PaperBlockData::new
        );
    }
}