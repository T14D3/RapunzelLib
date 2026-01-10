package de.t14d3.rapunzellib.network;

import org.jetbrains.annotations.NotNull;

public interface MessageListener {
    void onMessage(@NotNull String channel, @NotNull String data, @NotNull String sourceServer);
}

