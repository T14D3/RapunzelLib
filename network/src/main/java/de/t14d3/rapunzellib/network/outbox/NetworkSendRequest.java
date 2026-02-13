package de.t14d3.rapunzellib.network.outbox;

import de.t14d3.rapunzellib.network.NetworkEnvelope;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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
