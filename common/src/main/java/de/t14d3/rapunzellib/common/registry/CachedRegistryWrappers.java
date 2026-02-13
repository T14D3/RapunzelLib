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

public abstract class CachedRegistryWrappers<H, W extends RRegistryTypeHandle<H>> {
    private final ConcurrentHashMap<RKey, W> cache = new ConcurrentHashMap<>();

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

    protected final @NotNull W wrap(@NotNull RKey key, @NotNull H handle) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(handle, "handle");
        return cache.compute(key, (_ignored, existing) -> existing != null && isSameHandle(existing.handle(), handle)
            ? existing
            : createWrapper(key, handle));
    }

    protected boolean isSameHandle(@NotNull H existingHandle, @NotNull H newHandle) {
        return Objects.equals(existingHandle, newHandle);
    }

    protected abstract @NotNull W createWrapper(@NotNull RKey key, @NotNull H handle);
}
