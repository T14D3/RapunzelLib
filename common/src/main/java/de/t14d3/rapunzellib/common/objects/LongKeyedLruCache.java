package de.t14d3.rapunzellib.common.objects;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.LongFunction;

/**
 * A fixed-size, thread-safe LRU cache keyed by primitive {@code long} values.
 * <p>
 * Uses a {@link Long2ObjectLinkedOpenHashMap} for efficient long-keyed storage.
 * When the cache exceeds its maximum size, the eldest entry is evicted before
 * inserting a new one.
 *
 * @param <V> the value type
 */
public final class LongKeyedLruCache<V> {
    /** Maximum number of entries before eviction */
    private final int maxSize;
    /** The underlying access-ordered long-to-object map */
    private final Long2ObjectLinkedOpenHashMap<V> cache = new Long2ObjectLinkedOpenHashMap<>(16, 0.75f);

    /**
     * Creates a new LRU cache with the given maximum size.
     *
     * @param maxSize the maximum number of entries (must be positive)
     * @throws IllegalArgumentException if maxSize is not positive
     */
    public LongKeyedLruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.maxSize = maxSize;
    }

    /**
     * Retrieves the value for the given long key, creating it via the factory if not present.
     *
     * @param key     the lookup key
     * @param factory the function to create a new value if the key is absent
     * @return the existing or newly created value
     */
    public @NotNull V getOrCreate(long key, @NotNull LongFunction<? extends V> factory) {
        Objects.requireNonNull(factory, "factory");

        synchronized (cache) {
            V cached = cache.getAndMoveToLast(key);
            if (cached != null) {
                return cached;
            }
            if (cache.size() >= maxSize) {
                cache.removeFirst();
            }
            V created = Objects.requireNonNull(factory.apply(key), "factory result");
            cache.put(key, created);
            return created;
        }
    }
}
