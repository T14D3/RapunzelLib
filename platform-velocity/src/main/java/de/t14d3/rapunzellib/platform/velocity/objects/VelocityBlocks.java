package de.t14d3.rapunzellib.platform.velocity.objects;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;

import java.util.Optional;

public final class VelocityBlocks implements Blocks {
    @Override
    public Optional<RBlock> wrap(Object nativeBlock) {
        return Optional.empty();
    }

    @Override
    public Optional<RBlockData> wrapData(Object nativeBlockData) {
        return Optional.empty();
    }

    @Override
    public RBlock at(RWorld world, RBlockPos pos) {
        throw new UnsupportedOperationException("Blocks are not available on Velocity.");
    }

    @Override
    public Optional<RBlockData> parseData(String value) {
        return Optional.empty();
    }
}

