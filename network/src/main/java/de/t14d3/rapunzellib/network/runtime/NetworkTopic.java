package de.t14d3.rapunzellib.network.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record NetworkTopic<T>(@NotNull String channel, @NotNull Class<T> payloadType) {
    public NetworkTopic {
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel cannot be blank");
        }
        Objects.requireNonNull(payloadType, "payloadType");
    }

    public static <T> @NotNull NetworkTopic<T> of(@NotNull String channel, @NotNull Class<T> payloadType) {
        return new NetworkTopic<>(channel, payloadType);
    }
}
