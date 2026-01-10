package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface RNative {
    @NotNull PlatformId platformId();

    @NotNull Object handle();

    default <T> @NotNull T handle(@NotNull Class<T> type) {
        return type.cast(handle());
    }

    default <T> @NotNull Optional<T> tryHandle(@NotNull Class<T> type) {
        Object handle = handle();
        if (type.isInstance(handle)) return Optional.of(type.cast(handle));
        return Optional.empty();
    }

    /**
     * A small, per-wrapper key/value store intended for per-project extensions.
     * <p>
     * Platform implementations should return a mutable implementation so plugins can attach
     * additional data to wrapper instances.
     */
    default @NotNull RExtras extras() {
        return RExtras.empty();
    }
}

