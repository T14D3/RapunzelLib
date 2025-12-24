package de.t14d3.rapunzellib.objects;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface Players {
    Collection<RPlayer> online();

    Optional<RPlayer> get(UUID uuid);

    Optional<RPlayer> wrap(Object nativePlayer);

    default RPlayer require(UUID uuid) {
        return get(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown player: " + uuid));
    }

    default RPlayer require(Object nativePlayer) {
        return wrap(nativePlayer).orElseThrow(() -> new IllegalArgumentException("Cannot wrap player: " + nativePlayer));
    }
}

