package de.t14d3.rapunzellib.network;

import org.jetbrains.annotations.NotNull;

/**
 * Listener for incoming network messages.
 */
public interface MessageListener {
    /**
     * Called when a message is received on the given channel.
     *
     * @param channel the message channel
     * @param data the message payload
     * @param sourceServer the server that sent the message
     */
    void onMessage(@NotNull String channel, @NotNull String data, @NotNull String sourceServer);
}

