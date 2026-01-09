package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class NeoForgeBlocks implements Blocks {
    @Override
    public Optional<RBlock> wrap(Object nativeBlock) {
        return Optional.empty();
    }

    @Override
    public Optional<RBlockData> wrapData(Object nativeBlockData) {
        if (nativeBlockData instanceof BlockState state) return Optional.of(new NeoForgeBlockData(state));
        if (nativeBlockData instanceof Block block) return Optional.of(new NeoForgeBlockData(block.defaultBlockState()));
        return Optional.empty();
    }

    @Override
    public RBlock at(RWorld world, RBlockPos pos) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(pos, "pos");
        ServerLevel level = world.handle(ServerLevel.class);
        return new NeoForgeBlock(level, new BlockPos(pos.x(), pos.y(), pos.z()));
    }

    @Override
    public Optional<RBlockData> parseData(String value) {
        if (value == null) return Optional.empty();
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return Optional.empty();

        int bracketIndex = trimmed.indexOf('[');
        String id = (bracketIndex >= 0) ? trimmed.substring(0, bracketIndex) : trimmed;
        id = id.trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty()) return Optional.empty();

        ResourceLocation key = id.contains(":") ? ResourceLocation.tryParse(id) : ResourceLocation.withDefaultNamespace(id);
        if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) return Optional.empty();

        Block block = BuiltInRegistries.BLOCK.get(key).orElseThrow().value();
        return Optional.of(new NeoForgeBlockData(block.defaultBlockState()));
    }
}
