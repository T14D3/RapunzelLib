package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.network.outbox.NetworkOutboxPolicy;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration record for the network outbox queue.
 *
 * @param enabled      whether queuing is enabled
 * @param outboxPolicy the outbox policy governing store-and-forward decisions
 * @param flushPeriod  the period between flush cycles
 * @param maxBatchSize the maximum number of messages processed per flush
 * @param maxAge       the maximum age of messages before they expire
 */
public record NetworkQueueConfig(
    boolean enabled,
    NetworkOutboxPolicy outboxPolicy,
    Duration flushPeriod,
    int maxBatchSize,
    Duration maxAge
) {
    /**
     * Reads the network queue configuration from a YAML config.
     *
     * @param config the YAML configuration source
     * @return a new NetworkQueueConfig instance
     */
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

    /**
     * Generates a default owner ID based on the platform name and data directory.
     *
     * @return the default owner ID string
     */
    public static String defaultOwnerId() {
        return Rapunzel.context().platformId().name() + ":" +
            Rapunzel.context().dataDirectory().toAbsolutePath().normalize();
    }
}
