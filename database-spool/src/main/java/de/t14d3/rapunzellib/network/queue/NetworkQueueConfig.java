package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.network.outbox.NetworkOutboxPolicy;

import java.time.Duration;
import java.util.Objects;

public record NetworkQueueConfig(
    boolean enabled,
    NetworkOutboxPolicy outboxPolicy,
    Duration flushPeriod,
    int maxBatchSize,
    Duration maxAge
) {
    public static NetworkQueueConfig read(YamlConfig config) {
        Objects.requireNonNull(config, "config");

        boolean enabled = config.getBoolean("network.queue.enabled", true);

        long flushSeconds = Math.max(1L, config.getLong("network.queue.flushPeriodSeconds", 2));
        int maxBatchSize = (int) Math.max(1L, config.getLong("network.queue.maxBatchSize", 200));
        long maxAgeSeconds = Math.max(0L, config.getLong("network.queue.maxAgeSeconds", 300));

        return new NetworkQueueConfig(
            enabled,
            LegacyAllowlistNetworkOutboxPolicy.fromConfig(config),
            Duration.ofSeconds(flushSeconds),
            maxBatchSize,
            Duration.ofSeconds(maxAgeSeconds)
        );
    }

    public static String defaultOwnerId() {
        return Rapunzel.context().platformId().name() + ":" +
            Rapunzel.context().dataDirectory().toAbsolutePath().normalize();
    }
}
