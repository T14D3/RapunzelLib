package de.t14d3.rapunzellib.common.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

final class BlockCacheKeyTest {
    @Test
    void keyedCacheSeparatesLargePositiveCoordinateKeys() {
        KeyedLruCache<BlockCacheKey, Object> cache = new KeyedLruCache<>(4);
        BlockCacheKey origin = BlockCacheKey.of("minecraft:overworld", 0, 64, 0);
        BlockCacheKey wrappedPositive = BlockCacheKey.of("minecraft:overworld", 1024, 64, 0);

        Object first = cache.getOrCreate(origin, _key -> new Object());
        Object second = cache.getOrCreate(wrappedPositive, _key -> new Object());

        assertNotEquals(origin, wrappedPositive);
        assertNotSame(first, second);
        assertSame(first, cache.getOrCreate(origin, _key -> new Object()));
        assertSame(second, cache.getOrCreate(wrappedPositive, _key -> new Object()));
    }

    @Test
    void keyedCacheSeparatesLargeNegativeCoordinateKeys() {
        KeyedLruCache<BlockCacheKey, Object> cache = new KeyedLruCache<>(4);
        BlockCacheKey origin = BlockCacheKey.of("minecraft:overworld", 0, 64, 0);
        BlockCacheKey largeNegative = BlockCacheKey.of("minecraft:overworld", -2048, 64, 0);

        Object first = cache.getOrCreate(origin, _key -> new Object());
        Object second = cache.getOrCreate(largeNegative, _key -> new Object());

        assertNotEquals(origin, largeNegative);
        assertNotSame(first, second);
        assertSame(first, cache.getOrCreate(origin, _key -> new Object()));
        assertSame(second, cache.getOrCreate(largeNegative, _key -> new Object()));
    }

    @Test
    void keyedCacheRemainsWorldScopedForSameCoordinates() {
        KeyedLruCache<BlockCacheKey, Object> cache = new KeyedLruCache<>(4);
        BlockCacheKey overworld = BlockCacheKey.of("minecraft:overworld", 128, 70, -32);
        BlockCacheKey nether = BlockCacheKey.of("minecraft:the_nether", 128, 70, -32);

        Object first = cache.getOrCreate(overworld, _key -> new Object());
        Object second = cache.getOrCreate(nether, _key -> new Object());

        assertNotEquals(overworld, nether);
        assertNotSame(first, second);
        assertSame(first, cache.getOrCreate(overworld, _key -> new Object()));
        assertSame(second, cache.getOrCreate(nether, _key -> new Object()));
    }
}
