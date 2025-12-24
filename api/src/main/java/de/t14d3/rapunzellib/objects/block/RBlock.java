package de.t14d3.rapunzellib.objects.block;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.RWorld;

import java.util.Optional;

public interface RBlock extends RNative {
    RWorld world();

    RBlockPos pos();

    String typeKey();

    RBlockData data();

    static Optional<RBlock> wrap(Object nativeBlock) {
        return Rapunzel.blocks().wrap(nativeBlock);
    }

    static RBlock at(RWorld world, RBlockPos pos) {
        return Rapunzel.blocks().at(world, pos);
    }
}

