package de.t14d3.rapunzellib.objects.block;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;

import java.util.Optional;

public interface Blocks {
    Optional<RBlock> wrap(Object nativeBlock);

    Optional<RBlockData> wrapData(Object nativeBlockData);

    RBlock at(RWorld world, RBlockPos pos);

    Optional<RBlockData> parseData(String value);

    default RBlock require(Object nativeBlock) {
        return wrap(nativeBlock).orElseThrow(() -> new IllegalArgumentException("Cannot wrap block: " + nativeBlock));
    }

    default RBlockData requireData(Object nativeBlockData) {
        return wrapData(nativeBlockData)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap block data: " + nativeBlockData));
    }
}

