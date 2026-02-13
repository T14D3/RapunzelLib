package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedBlockDataBase;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

final class NeoForgeBlockData extends SharedBlockDataBase {
    NeoForgeBlockData(BlockState state) {
        super(PlatformId.NEOFORGE, Objects.requireNonNull(state, "state"));
    }
}
