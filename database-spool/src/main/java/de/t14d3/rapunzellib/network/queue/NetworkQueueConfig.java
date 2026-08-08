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
     * <p>The queue is only enabled by default when a real backing store is
     * configured ({@code network.queue.jdbc} or {@code database.jdbc}); with
     * an empty/absent database section the default is {@code false} so an
     * unconfigured platform does not silently create an in-memory outbox that
     * consumers then re-wrap. An explicit {@code network.queue.enabled} value
     * always wins.</p>
     *
     * @param config the YAML configuration source
     * @return a new NetworkQueueConfig instance
     */
    public static NetworkQueueConfig read(YamlConfig config) {
        Objects.requireNonNull(config, "config");

        boolean enabled = config.getBoolean("network.queue.enabled", hasBackingStore(config));

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
     * Determines whether a queue backing store is configured.
     *
     * @param config the YAML configuration source
     * @return {@code true} if either queue or database JDBC is configured
     */
    private static boolean hasBackingStore(YamlConfig config) {
        return isNonBlank(config.getString("network.queue.jdbc", null))
            || isNonBlank(config.getString("database.jdbc", null));
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
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
