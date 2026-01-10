package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface Players {
    @NotNull Collection<RPlayer> online();

    @NotNull Optional<RPlayer> get(@NotNull UUID uuid);

    @NotNull Optional<RPlayer> wrap(@NotNull Object nativePlayer);

    default @NotNull RPlayer require(@NotNull UUID uuid) {
        return get(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown player: " + uuid));
    }

    default @NotNull RPlayer require(@NotNull Object nativePlayer) {
        return wrap(nativePlayer).orElseThrow(() -> new IllegalArgumentException("Cannot wrap player: " + nativePlayer));
    }
}

