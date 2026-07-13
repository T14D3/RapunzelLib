package de.t14d3.rapunzellib.common.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for stores that maintain a concurrent cache wrapping native handles.
 * <p>
 * Provides automatic cache management: native handles are wrapped on first access and
 * the wrapper is updated on subsequent accesses with the same key.
 *
 * @param <K> the cache key type (e.g. UUID)
 * @param <N> the native handle type
 * @param <W> the wrapper type
 */
public abstract class CachedWrapperStore<K, N, W> {
    private final ConcurrentHashMap<K, W> cache = new ConcurrentHashMap<>();

    protected final @NotNull W wrapCached(@NotNull K key, @NotNull N nativeHandle) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(nativeHandle, "nativeHandle");
        return cache.compute(key, (_ignored, existing) -> {
            if (existing == null) {
                return createWrapper(nativeHandle);
            }
            updateWrapper(existing, nativeHandle);
            return existing;
        });
    }

    protected abstract @NotNull W createWrapper(@NotNull N nativeHandle);

    protected abstract void updateWrapper(@NotNull W existingWrapper, @NotNull N nativeHandle);
}
