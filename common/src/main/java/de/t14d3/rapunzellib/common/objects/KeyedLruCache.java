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
    private final int maxSize;
    private final Map<K, V> cache;

    public KeyedLruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<>(16, 0.75f, true);
    }

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
