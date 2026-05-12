package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.common.objects.BlockCacheKey;
import de.t14d3.rapunzellib.common.objects.KeyedLruCache;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Abstract base implementation of {@link Blocks} providing block wrapping and caching.
 * <p>
 * Maintains an LRU cache of wrapped block instances keyed by world key and block position.
 * Supports wrapping native block objects, block data parsing, and position-based lookup.
 * </p>
 *
 * @param <B> the concrete block wrapper type
 * @param <D> the concrete block data wrapper type
 */
public abstract class SharedBlocksCore<B extends RBlock, D extends RBlockData> implements Blocks {
    private static final int CACHE_SIZE = 10_000;

    private final KeyedLruCache<BlockCacheKey, B> blockCache = new KeyedLruCache<>(CACHE_SIZE);

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock) {
        Objects.requireNonNull(nativeBlock, "nativeBlock");
        return adaptNativeBlock(nativeBlock).flatMap(ref -> wrapNative(ref.level(), ref.pos())).map(RBlock.class::cast);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<RBlockData> wrapData(@NotNull Object nativeBlockData) {
        if (nativeBlockData instanceof BlockState state) return Optional.of(createBlockData(state));
        if (nativeBlockData instanceof Block block) return Optional.of(createBlockData(block.defaultBlockState()));
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(pos, "pos");
        return atNative(world.handle(ServerLevel.class), new BlockPos(pos.x(), pos.y(), pos.z()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<RBlockData> parseData(@NotNull String value) {
        return SharedBlockStateSupport.parse(value).map(this::createBlockData);
    }

    /**
     * Creates a new block wrapper for the given server level and position.
     *
     * @param level the server level
     * @param pos   the block position
     * @return the new block wrapper instance
     */
    protected abstract @NotNull B createBlock(@NotNull ServerLevel level, @NotNull BlockPos pos);

    /**
     * Creates a new block data wrapper for the given block state.
     *
     * @param state the block state
     * @return the new block data wrapper instance
     */
    protected abstract @NotNull D createBlockData(@NotNull BlockState state);

    /**
     * Wraps a native level and position into an Optional containing the managed block wrapper type.
     *
     * @param level the server level
     * @param pos   the block position
     * @return an Optional containing the wrapped block
     */
    public final @NotNull Optional<B> wrapNative(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        return Optional.of(atNative(level, pos));
    }

    /**
     * Attempts to adapt a generic native object into a {@link NativeBlockRef}.
     * <p>
     * Default implementation returns empty; subclasses should override to provide
     * actual adaptation logic.
     * </p>
     *
     * @param nativeBlock the object to adapt
     * @return an Optional containing the adapted block reference, or empty if not adaptable
     */
    protected @NotNull Optional<NativeBlockRef> adaptNativeBlock(@NotNull Object nativeBlock) {
        return Optional.empty();
    }

    /**
     * Returns a cached or newly created block wrapper for the given native level and position.
     *
     * @param level the server level
     * @param pos   the block position
     * @return the cached or newly created block wrapper
     */
    protected final @NotNull B atNative(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        BlockCacheKey key = BlockCacheKey.of(SharedWorldHooks.key(level), pos.getX(), pos.getY(), pos.getZ());
        return blockCache.getOrCreate(key, ignored -> createBlock(level, pos));
    }

    /**
     * Record holding a server level and block position reference from native adaptation.
     *
     * @param level the server level
     * @param pos   the block position
     */
    protected record NativeBlockRef(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        public NativeBlockRef {
            Objects.requireNonNull(level, "level");
            Objects.requireNonNull(pos, "pos");
        }
    }
}
