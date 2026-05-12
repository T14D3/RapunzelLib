package de.t14d3.rapunzellib.common.objects;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A fixed-size, thread-safe LRU (Least Recently Used) cache keyed by arbitrary types.
 * <p>
 * When the cache exceeds its maximum size, the eldest entry is evicted before
 * inserting a new one. All operations are synchronized on the internal map.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class KeyedLruCache<K, V> {
    /** Maximum number of entries before eviction */
    private final int maxSize;
    /** The underlying access-ordered linked hash map */
    private final Map<K, V> cache;

    /**
     * Creates a new LRU cache with the given maximum size.
     *
     * @param maxSize the maximum number of entries (must be positive)
     * @throws IllegalArgumentException if maxSize is not positive
     */
    public KeyedLruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<>(16, 0.75f, true);
    }

    /**
     * Retrieves the value for the given key, creating it via the factory if not present.
     *
     * @param key     the lookup key
     * @param factory the function to create a new value if the key is absent
     * @return the existing or newly created value
     */
    public @NotNull V getOrCreate(@NotNull K key, @NotNull Function<? super K, ? extends V> factory) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(factory, "factory");

        synchronized (cache) {
            V cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
            if (cache.size() >= maxSize) {
                K eldestKey = cache.keySet().iterator().next();
                cache.remove(eldestKey);
            }
            V created = Objects.requireNonNull(factory.apply(key), "factory result");
            cache.put(key, created);
            return created;
        }
    }
}
