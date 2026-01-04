package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RAudience;

import java.util.Optional;

public interface RCommandSource extends RAudience {
    Optional<RPlayer> player();

    default boolean isPlayer() {
        return player().isPresent();
    }

    default RPlayer requirePlayer() {
        return player().orElseThrow(() -> new IllegalStateException("Command source is not a player"));
    }
}
