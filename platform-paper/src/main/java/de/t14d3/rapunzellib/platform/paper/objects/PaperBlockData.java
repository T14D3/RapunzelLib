package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedBlockDataBase;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

final class PaperBlockData extends SharedBlockDataBase {
    PaperBlockData(BlockState data) {
        super(PlatformId.PAPER, Objects.requireNonNull(data, "data"));
    }
}
