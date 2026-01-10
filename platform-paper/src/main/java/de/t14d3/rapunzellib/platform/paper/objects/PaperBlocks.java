package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class PaperBlocks implements Blocks {
    // LRU cache for block wrappers using primitive longs (zero allocation lookups)
    // Key format: world.hashCode() in upper 32 bits, block coords in lower 32 bits
    private static final int CACHE_SIZE = 10_000;
    private final Long2ObjectLinkedOpenHashMap<PaperBlock> blockCache = new Long2ObjectLinkedOpenHashMap<>(16, 0.75f);

    private static long cacheKey(String worldName, int x, int y, int z) {
        // Pack world hash + coordinates into a long
        // This avoids allocating BlockCacheKey objects on every lookup
        long worldHash = (long) worldName.hashCode() << 32;
        // Pack x, y, z into lower 32 bits (10 bits each, supporting -512 to 511 per axis)
        int packedPos = ((x & 0x3FF) << 20) | ((y & 0x3FF) << 10) | (z & 0x3FF);
        return worldHash | (packedPos & 0xFFFFFFFFL);
    }

    @Override
    public @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock) {
        if (!(nativeBlock instanceof Block block)) return Optional.empty();
        return Optional.of(wrapInternal(block));
    }

    private PaperBlock wrapInternal(Block block) {
        long key = cacheKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        synchronized (blockCache) {
            PaperBlock cached = blockCache.getAndMoveToLast(key);
            if (cached != null) {
                cached.updateNativeHandle(block);
                return cached;
            }
            
            // Evict oldest entry if cache is full
            if (blockCache.size() >= CACHE_SIZE) {
                blockCache.removeFirst();
            }
            
            PaperBlock newBlock = new PaperBlock(block);
            blockCache.put(key, newBlock);
            return newBlock;
        }
    }

    @Override
    public @NotNull Optional<RBlockData> wrapData(@NotNull Object nativeBlockData) {
        if (nativeBlockData instanceof BlockData data) return Optional.of(new PaperBlockData(data));
        if (nativeBlockData instanceof Block block) return Optional.of(new PaperBlockData(block.getBlockData()));
        return Optional.empty();
    }

    @Override
    public @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(pos, "pos");
        World bukkitWorld = world.handle(World.class);

        long key = cacheKey(bukkitWorld.getName(), pos.x(), pos.y(), pos.z());
        synchronized (blockCache) {
            PaperBlock cached = blockCache.getAndMoveToLast(key);
            if (cached != null) {
                // Refresh handle in case block object changed
                Block block = bukkitWorld.getBlockAt(pos.x(), pos.y(), pos.z());
                cached.updateNativeHandle(block);
                return cached;
            }
            
            // Evict oldest entry if cache is full
            if (blockCache.size() >= CACHE_SIZE) {
                blockCache.removeFirst();
            }
            
            Block block = bukkitWorld.getBlockAt(pos.x(), pos.y(), pos.z());
            PaperBlock newBlock = new PaperBlock(block);
            blockCache.put(key, newBlock);
            return newBlock;
        }
    }

    @Override
    public @NotNull Optional<RBlockData> parseData(@NotNull String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return Optional.empty();

        try {
            return Optional.of(new PaperBlockData(Bukkit.createBlockData(trimmed)));
        } catch (IllegalArgumentException ignored) {
            // try Material parsing next
        }

        Material material = Material.matchMaterial(trimmed);
        if (material == null) {
            try {
                material = Material.valueOf(trimmed.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }

        if (!material.isBlock()) return Optional.empty();
        return Optional.of(new PaperBlockData(material.createBlockData()));
    }
}

