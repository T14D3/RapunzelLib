package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedBlockDataBase;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

final class FabricBlockData extends SharedBlockDataBase {
    FabricBlockData(BlockState state) {
        super(PlatformId.FABRIC, Objects.requireNonNull(state, "state"));
    }
}
