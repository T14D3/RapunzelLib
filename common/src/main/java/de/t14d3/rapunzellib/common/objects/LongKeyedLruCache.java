package de.t14d3.rapunzellib.common.objects;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.LongFunction;

public final class LongKeyedLruCache<V> {
    private final int maxSize;
    private final Long2ObjectLinkedOpenHashMap<V> cache = new Long2ObjectLinkedOpenHashMap<>(16, 0.75f);

    public LongKeyedLruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.maxSize = maxSize;
    }

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
