package de.t14d3.rapunzellib.common.objects;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A composite key for caching block data, combining a world identifier with
 * three-dimensional block coordinates.
 *
 * @param worldKey the namespaced key of the world as a string
 * @param x        the block x-coordinate
 * @param y        the block y-coordinate
 * @param z        the block z-coordinate
 */
public record BlockCacheKey(@NotNull String worldKey, int x, int y, int z) {
    public BlockCacheKey {
        worldKey = Objects.requireNonNull(worldKey, "worldKey");
    }

    /**
     * Creates a BlockCacheKey from an {@link RKey} world identifier and coordinates.
     *
     * @param worldKey the world key
     * @param x        the block x-coordinate
     * @param y        the block y-coordinate
     * @param z        the block z-coordinate
     * @return a new BlockCacheKey
     */
    public static @NotNull BlockCacheKey of(@NotNull RKey worldKey, int x, int y, int z) {
        return new BlockCacheKey(worldKey.asString(), x, y, z);
    }

    /**
     * Creates a BlockCacheKey from a string world key and coordinates.
     *
     * @param worldKey the world key as a string
     * @param x        the block x-coordinate
     * @param y        the block y-coordinate
     * @param z        the block z-coordinate
     * @return a new BlockCacheKey
     */
    public static @NotNull BlockCacheKey of(@NotNull String worldKey, int x, int y, int z) {
        return new BlockCacheKey(worldKey, x, y, z);
    }
}
