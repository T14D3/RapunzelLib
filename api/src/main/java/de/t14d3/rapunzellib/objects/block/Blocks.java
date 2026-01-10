package de.t14d3.rapunzellib.objects.block;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Blocks {
    @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock);

    @NotNull Optional<RBlockData> wrapData(@NotNull Object nativeBlockData);

    @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos);

    @NotNull Optional<RBlockData> parseData(@NotNull String value);

    default @NotNull RBlock require(@NotNull Object nativeBlock) {
        return wrap(nativeBlock).orElseThrow(() -> new IllegalArgumentException("Cannot wrap block: " + nativeBlock));
    }

    default @NotNull RBlockData requireData(@NotNull Object nativeBlockData) {
        return wrapData(nativeBlockData)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap block data: " + nativeBlockData));
    }
}

