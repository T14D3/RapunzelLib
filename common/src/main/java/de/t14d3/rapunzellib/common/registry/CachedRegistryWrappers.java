package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Abstract base for registries that cache wrapper objects around native handles.
 * <p>
 * Provides concurrent caching with identity-based handle comparison to avoid
 * unnecessary wrapper re-creation when the handle instance hasn't changed.
 *
 * @param <H> the native handle type
 * @param <W> the wrapper type, extending {@link RRegistryTypeHandle}
 */
public abstract class CachedRegistryWrappers<H, W extends RRegistryTypeHandle<H>> {
    /** Concurrent cache mapping registry keys to wrapper instances */
    private final ConcurrentHashMap<RKey, W> cache = new ConcurrentHashMap<>();

    /**
     * Finds a wrapped entry by key and handle, verifying the handle's key matches.
     *
     * @param requestedKey the requested registry key
     * @param handle       the native handle (may be null)
     * @param keyResolver  extracts the registry key from a handle
     * @return an optional containing the wrapped entry, or empty if handle is null or key mismatched
     */
    protected final @NotNull Optional<W> findWrapped(
        @NotNull RKey requestedKey,
        @Nullable H handle,
        @NotNull Function<? super H, RKey> keyResolver
    ) {
        Objects.requireNonNull(requestedKey, "requestedKey");
        Objects.requireNonNull(keyResolver, "keyResolver");
        if (handle == null) {
            return Optional.empty();
        }
        if (!requestedKey.equals(keyResolver.apply(handle))) {
            return Optional.empty();
        }
        return Optional.of(wrap(requestedKey, handle));
    }

    /**
     * Wraps all entries from an iterable of handles into the given view type.
     *
     * @param handles    the native handles to wrap
     * @param keyResolver extracts the registry key from a handle
     * @param viewType   the target view type class
     * @param <T>        the view type
     * @return an immutable list of wrapped entries
     */
    protected final <T> @NotNull List<T> wrapEntries(
        @NotNull Iterable<? extends H> handles,
        @NotNull Function<? super H, RKey> keyResolver,
        @NotNull Class<T> viewType
    ) {
        Objects.requireNonNull(handles, "handles");
        Objects.requireNonNull(keyResolver, "keyResolver");
        Objects.requireNonNull(viewType, "viewType");

        List<T> entries = new ArrayList<>();
        for (H handle : handles) {
            if (handle == null) {
                continue;
            }
            entries.add(viewType.cast(wrap(keyResolver.apply(handle), handle)));
        }
        return List.copyOf(entries);
    }

    /**
     * Wraps a native handle, reusing the cached wrapper if the handle is unchanged.
     *
     * @param key    the registry key
     * @param handle the native handle
     * @return the (possibly cached) wrapper
     */
    protected final @NotNull W wrap(@NotNull RKey key, @NotNull H handle) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(handle, "handle");
        return cache.compute(key, (_ignored, existing) -> existing != null && isSameHandle(existing.handle(), handle)
            ? existing
            : createWrapper(key, handle));
    }

    /**
     * Checks whether an existing handle is the same as a new one.
     *
     * @param existingHandle the existing handle
     * @param newHandle      the new handle
     * @return true if they are the same
     */
    protected boolean isSameHandle(@NotNull H existingHandle, @NotNull H newHandle) {
        return Objects.equals(existingHandle, newHandle);
    }

    /**
     * Creates a new wrapper for the given key and handle.
     *
     * @param key    the registry key
     * @param handle the native handle
     * @return a new wrapper
     */
    protected abstract @NotNull W createWrapper(@NotNull RKey key, @NotNull H handle);
}
