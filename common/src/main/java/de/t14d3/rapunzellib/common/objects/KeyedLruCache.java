package de.t14d3.rapunzellib.common.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
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

    /**
     * Returns the value cached for the given key, or {@code null} if absent.
     * A hit counts as a use for LRU eviction purposes.
     */
    public @Nullable V get(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        synchronized (cache) {
            return cache.get(key);
        }
    }

    /**
     * Associates the given value with the key, evicting the least recently used
     * entry first when the cache is at capacity. Returns the value previously
     * associated with the key, or {@code null} if there was none.
     */
    public @Nullable V put(@NotNull K key, @NotNull V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        synchronized (cache) {
            V previous = cache.get(key);
            if (previous == null && cache.size() >= maxSize) {
                K eldestKey = cache.keySet().iterator().next();
                cache.remove(eldestKey);
            }
            cache.put(key, value);
            return previous;
        }
    }

    /**
     * Removes the entry for the given key. Returns the removed value, or
     * {@code null} if the key was absent.
     */
    public @Nullable V remove(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        synchronized (cache) {
            return cache.remove(key);
        }
    }

    /**
     * Atomically computes a value for the given key, mirroring
     * {@link java.util.concurrent.ConcurrentHashMap#compute(Object, BiFunction)}:
     * the remapping function receives the existing value (or {@code null}) and
     * its result is stored; a {@code null} result removes the entry. When a new
     * entry is inserted at capacity, the least recently used entry is evicted.
     *
     * @param remappingFunction the function to compute a value for the key
     * @return the computed value, or {@code null} if the function returned {@code null}
     */
    public @Nullable V compute(@NotNull K key,
                               @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(remappingFunction, "remappingFunction");
        synchronized (cache) {
            V existing = cache.get(key);
            V computed = remappingFunction.apply(key, existing);
            if (computed == null) {
                if (existing != null) {
                    cache.remove(key);
                }
                return null;
            }
            if (existing == null && cache.size() >= maxSize) {
                K eldestKey = cache.keySet().iterator().next();
                cache.remove(eldestKey);
            }
            cache.put(key, computed);
            return computed;
        }
    }
}
