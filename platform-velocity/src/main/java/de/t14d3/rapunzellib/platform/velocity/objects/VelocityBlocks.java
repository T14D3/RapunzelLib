package de.t14d3.rapunzellib.platform.velocity.objects;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class VelocityBlocks implements Blocks {
    @Override
    public @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock) {
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<RBlockData> wrapData(@NotNull Object nativeBlockData) {
        return Optional.empty();
    }

    @Override
    public @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos) {
        throw new UnsupportedOperationException("Blocks are not available on Velocity.");
    }

    @Override
    public @NotNull Optional<RBlockData> parseData(@NotNull String value) {
        return Optional.empty();
    }
}

