package de.t14d3.rapunzellib.objects.block;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RNative;

import java.util.Optional;

public interface RBlockData extends RNative {
    String typeKey();

    String asString();

    static Optional<RBlockData> wrap(Object nativeBlockData) {
        return Rapunzel.blocks().wrapData(nativeBlockData);
    }

    static Optional<RBlockData> parse(String value) {
        return Rapunzel.blocks().parseData(value);
    }
}

