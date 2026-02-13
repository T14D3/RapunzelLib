package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface RProxyPlayer extends RPlayer {
    @NotNull Optional<String> currentServerName();

    default @NotNull String currentServerNameOrThrow() {
        return currentServerName().orElseThrow(() -> new UnsupportedOperationException("currentServerName is not supported for " + getClass().getName()));
    }
}
