package de.t14d3.rapunzellib.objects.block;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RNative;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface RBlockData extends RNative {
    @NotNull String typeKey();

    @NotNull String asString();

    static @NotNull Optional<RBlockData> wrap(@NotNull Object nativeBlockData) {
        return Rapunzel.blocks().wrapData(nativeBlockData);
    }

    static @NotNull Optional<RBlockData> parse(@NotNull String value) {
        return Rapunzel.blocks().parseData(value);
    }
}

