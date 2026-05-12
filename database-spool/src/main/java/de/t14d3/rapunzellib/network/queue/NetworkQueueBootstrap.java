package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.outbox.NetworkOutboxPolicy;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Bootstrap helper for wrapping a {@link Messenger} in a {@link DbQueuedMessenger}
 * based on config.
 *
 * <p>Queueing is primarily useful for transports that have transient delivery
 * constraints (e.g. plugin messaging needing a player carrier).</p>
 */
public final class NetworkQueueBootstrap {
    private NetworkQueueBootstrap() {
    }

    /**
     * Result of the bootstrap process.
     *
     * @param messenger the (possibly wrapped) messenger
     * @param closeable a closeable to clean up resources, or null if no wrapping was applied
     */
    public record Result(Messenger messenger, AutoCloseable closeable) {
    }

    /**
     * Wraps the delegate messenger in a {@link DbQueuedMessenger} if the config enables queueing.
     *
     * @param delegate  the underlying messenger
     * @param config    the YAML config
     * @param scheduler the scheduler for periodic flush tasks
     * @param logger    the logger
     * @param ownerId   the owner identifier for this instance
     * @return the bootstrap result containing the (possibly wrapped) messenger
     */
    public static Result wrapIfEnabled(
        Messenger delegate,
        de.t14d3.rapunzellib.config.YamlConfig config,
        Scheduler scheduler,
        Logger logger,
        String ownerId
    ) {
        return wrapIfEnabled(delegate, config, scheduler, logger, ownerId, null, null, null);
    }

    /**
     * Full wrapper method with optional server resolution hooks and listener.
     *
     * @param delegate                  the underlying messenger
     * @param config                    the YAML config
     * @param scheduler                 the scheduler for periodic flush tasks
     * @param logger                    the logger
     * @param ownerId                   the owner identifier for this instance
     * @param allServersSupplier        supplier for the list of all known servers, or null
     * @param canSendToServerOverride   predicate to override send-to-server decisions, or null
     * @param listener                  the lifecycle listener, or null
     * @return the bootstrap result
     */
    public static Result wrapIfEnabled(
        Messenger delegate,
        de.t14d3.rapunzellib.config.YamlConfig config,
        Scheduler scheduler,
        Logger logger,
        String ownerId,
        Supplier<List<String>> allServersSupplier,
        Predicate<String> canSendToServerOverride,
        DbQueuedMessenger.Listener listener
    ) {
        Objects.requireNonNull(delegate, "delegate");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(ownerId, "ownerId");

        NetworkQueueConfig queue = NetworkQueueConfig.read(config);
        if (!queue.enabled()) {
            return new Result(delegate, null);
        }

        String jdbc =
            normalizeJdbc(config.getString("network.queue.jdbc", null));
        if (jdbc == null) {
            jdbc = normalizeJdbc(config.getString("database.jdbc", null));
        }

        DbQueuedMessenger.OutboxStore store = null;
        if (jdbc != null) {
            try {
                SpoolDatabase db = SpoolDatabase.builder()
                    .jdbcUrl(jdbc)
                    .logger(logger)
                    .entities(NetworkOutboxMessage.class)
                    .build();
                store = new DbQueuedMessenger.DatabaseOutboxStore(db);
            } catch (Exception e) {
                logger.warn("Failed to initialize outbox DB; falling back to in-memory queueing", e);
            }
        }
        if (store == null) {
            store = new DbQueuedMessenger.InMemoryOutboxStore();
        }

        NetworkOutboxPolicy outboxPolicy = queue.outboxPolicy();
        Duration flushPeriod = queue.flushPeriod();
        int maxBatchSize = queue.maxBatchSize();
        Duration maxAge = queue.maxAge();

        DbQueuedMessenger queued = new DbQueuedMessenger(
            store,
            delegate,
            scheduler,
            logger,
            ownerId,
            outboxPolicy,
            flushPeriod,
            maxBatchSize,
            maxAge,
            allServersSupplier,
            canSendToServerOverride,
            listener
        );
        return new Result(queued, queued);
    }

    /**
     * Normalizes a JDBC URL string by trimming and checking for blank.
     *
     * @param value the raw JDBC URL
     * @return the normalized URL, or null if blank
     */
    private static String normalizeJdbc(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isBlank()) return null;
        return trimmed;
    }
}
