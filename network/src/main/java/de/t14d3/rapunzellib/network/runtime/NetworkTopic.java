package de.t14d3.rapunzellib.network.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A typed network topic identified by a channel string.
 *
 * <p>Combines a channel name with the expected payload type for type-safe
 * pub/sub messaging over the network.
 *
 * @param channel     the channel name
 * @param payloadType the expected payload class
 * @param <T>         the payload type
 */
public record NetworkTopic<T>(@NotNull String channel, @NotNull Class<T> payloadType) {
    public NetworkTopic {
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel cannot be blank");
        }
        Objects.requireNonNull(payloadType, "payloadType");
    }

    /**
     * Creates a typed network topic.
     *
     * @param channel     the channel name (must not be blank)
     * @param payloadType the expected payload class
     * @param <T>         the payload type
     * @return the new topic
     */
    public static <T> @NotNull NetworkTopic<T> of(@NotNull String channel, @NotNull Class<T> payloadType) {
        return new NetworkTopic<>(channel, payloadType);
    }
}
