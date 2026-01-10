package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import java.util.Objects;
import java.util.Optional;

public final class SpongeBlocks implements Blocks {
    // LRU cache for block wrappers using primitive longs (zero allocation lookups)
    private static final int CACHE_SIZE = 10_000;
    private final Long2ObjectLinkedOpenHashMap<SpongeBlock> blockCache = new Long2ObjectLinkedOpenHashMap<>(16, 0.75f);

    private static long cacheKey(String worldKey, int x, int y, int z) {
        long worldHash = (long) worldKey.hashCode() << 32;
        int packedPos = ((x & 0x3FF) << 20) | ((y & 0x3FF) << 10) | (z & 0x3FF);
        return worldHash | (packedPos & 0xFFFFFFFFL);
    }

    @Override
    public @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock) {
        switch (nativeBlock) {
            case ServerLocation loc -> {
                return Optional.of(wrapInternal(loc.world(), loc.blockPosition()));
            }
            case LocatableBlock locatable -> {
                if (!(locatable.world() instanceof ServerWorld world)) return Optional.empty();
                return Optional.of(wrapInternal(world, locatable.blockPosition()));
            }
            case BlockSnapshot snapshot -> {
                if (!Sponge.isServerAvailable()) return Optional.empty();
                Vector3i pos = snapshot.position();
                return Sponge.server().worldManager().world(snapshot.world())
                        .map(world -> wrapInternal(world, pos));
            }
            default -> {
            }
        }
        return Optional.empty();
    }

    private SpongeBlock wrapInternal(ServerWorld world, Vector3i pos) {
        long key = cacheKey(world.key().asString(), pos.x(), pos.y(), pos.z());
        synchronized (blockCache) {
            SpongeBlock cached = blockCache.getAndMoveToLast(key);
            if (cached != null) {
                return cached;
            }
            
            if (blockCache.size() >= CACHE_SIZE) {
                blockCache.removeFirst();
            }
            
            SpongeBlock newBlock = new SpongeBlock(world, pos);
            blockCache.put(key, newBlock);
            return newBlock;
        }
    }

    @Override
    public @NotNull Optional<RBlockData> wrapData(@NotNull Object nativeBlockData) {
        return switch (nativeBlockData) {
            case BlockState state -> Optional.of(new SpongeBlockData(state));
            case BlockType type -> Optional.of(new SpongeBlockData(type.defaultState()));
            case BlockSnapshot snapshot -> Optional.of(new SpongeBlockData(snapshot.state()));
            case LocatableBlock locatable -> Optional.of(new SpongeBlockData(locatable.blockState()));
            case ServerLocation loc -> Optional.of(new SpongeBlockData(loc.block()));
            default -> Optional.empty();
        };
    }

    @Override
    public @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(pos, "pos");
        ServerWorld spongeWorld = world.handle(ServerWorld.class);

        long key = cacheKey(spongeWorld.key().asString(), pos.x(), pos.y(), pos.z());
        synchronized (blockCache) {
            SpongeBlock cached = blockCache.getAndMoveToLast(key);
            if (cached != null) {
                return cached;
            }
            
            if (blockCache.size() >= CACHE_SIZE) {
                blockCache.removeFirst();
            }
            
            SpongeBlock newBlock = new SpongeBlock(spongeWorld, new Vector3i(pos.x(), pos.y(), pos.z()));
            blockCache.put(key, newBlock);
            return newBlock;
        }
    }

    @Override
    public @NotNull Optional<RBlockData> parseData(@NotNull String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return Optional.empty();

        try {
            BlockState state = BlockState.fromString(trimmed);
            return Optional.of(new SpongeBlockData(state));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
