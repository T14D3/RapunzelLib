package de.t14d3.rapunzellib.objects.block;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.RWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface RBlock extends RNative {
    @NotNull RWorld world();

    @NotNull RBlockPos pos();

    @NotNull String typeKey();

    @NotNull RBlockData data();

    static @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock) {
        return Rapunzel.blocks().wrap(nativeBlock);
    }

    static @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos) {
        return Rapunzel.blocks().at(world, pos);
    }
}

