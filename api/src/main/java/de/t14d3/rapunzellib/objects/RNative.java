package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;

import java.util.Optional;

public interface RNative {
    PlatformId platformId();

    Object handle();

    default <T> T handle(Class<T> type) {
        return type.cast(handle());
    }

    default <T> Optional<T> tryHandle(Class<T> type) {
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
    default RExtras extras() {
        return RExtras.empty();
    }
}

