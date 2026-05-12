package de.t14d3.rapunzellib.network.outbox;

import de.t14d3.rapunzellib.network.NetworkEnvelope;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a request to send a message over the network.
 *
 * @param target       the routing target
 * @param targetServer the specific target server (may be null)
 * @param channel      the message channel
 * @param data         the serialized message payload
 */
public record NetworkSendRequest(
    @NotNull NetworkEnvelope.Target target,
    String targetServer,
    @NotNull String channel,
    String data
) {
    public NetworkSendRequest {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(channel, "channel");
    }
}
