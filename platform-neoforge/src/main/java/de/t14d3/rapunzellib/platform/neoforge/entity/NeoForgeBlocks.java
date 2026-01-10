package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class NeoForgeBlocks implements Blocks {
    // LRU cache for block wrappers using primitive longs (zero allocation lookups)
    private static final int CACHE_SIZE = 10_000;
    private final Long2ObjectLinkedOpenHashMap<NeoForgeBlock> blockCache = new Long2ObjectLinkedOpenHashMap<>(16, 0.75f);

    private static long cacheKey(String worldId, int x, int y, int z) {
        long worldHash = (long) worldId.hashCode() << 32;
        int packedPos = ((x & 0x3FF) << 20) | ((y & 0x3FF) << 10) | (z & 0x3FF);
        return worldHash | (packedPos & 0xFFFFFFFFL);
    }

    @Override
    public @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock) {
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<RBlockData> wrapData(@NotNull Object nativeBlockData) {
        if (nativeBlockData instanceof BlockState state) return Optional.of(new NeoForgeBlockData(state));
        if (nativeBlockData instanceof Block block) return Optional.of(new NeoForgeBlockData(block.defaultBlockState()));
        return Optional.empty();
    }

    @Override
    public @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(pos, "pos");
        ServerLevel level = world.handle(ServerLevel.class);

        long key = cacheKey(level.dimension().location().toString(), pos.x(), pos.y(), pos.z());
        synchronized (blockCache) {
            NeoForgeBlock cached = blockCache.getAndMoveToLast(key);
            if (cached != null) {
                return cached;
            }

            if (blockCache.size() >= CACHE_SIZE) {
                blockCache.removeFirst();
            }

            NeoForgeBlock newBlock = new NeoForgeBlock(level, new BlockPos(pos.x(), pos.y(), pos.z()));
            blockCache.put(key, newBlock);
            return newBlock;
        }
    }

    @Override
    public @NotNull Optional<RBlockData> parseData(@NotNull String value) {
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
