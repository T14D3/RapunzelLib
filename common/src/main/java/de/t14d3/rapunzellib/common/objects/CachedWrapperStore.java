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
    /** Concurrent cache mapping keys to wrapper instances */
    private final ConcurrentHashMap<K, W> cache = new ConcurrentHashMap<>();

    /**
     * Wraps or retrieves a cached wrapper for the given native handle.
     * If no wrapper exists for the key, a new one is created. Otherwise the
     * existing wrapper is updated with the new native handle.
     *
     * @param key          the cache key
     * @param nativeHandle the native handle to wrap
     * @return an existing or newly created wrapper
     */
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

    /**
     * Creates a new wrapper for the given native handle.
     *
     * @param nativeHandle the native handle to wrap
     * @return a new wrapper instance
     */
    protected abstract @NotNull W createWrapper(@NotNull N nativeHandle);

    /**
     * Updates an existing wrapper with a new native handle.
     *
     * @param existingWrapper the existing wrapper to update
     * @param nativeHandle    the new native handle
     */
    protected abstract void updateWrapper(@NotNull W existingWrapper, @NotNull N nativeHandle);
}
