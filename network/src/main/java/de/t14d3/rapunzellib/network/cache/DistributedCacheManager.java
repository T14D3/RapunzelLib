package de.t14d3.rapunzellib.network.cache;

import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkTopic;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Distributed cache manager that handles cross-server cache invalidation via Redis pub/sub.
 * <p>
 * This class implements the {@code CacheEventSink} interface from Spool to integrate with
 * the ORM's caching layer. It uses the existing {@link Messenger} infrastructure (typically
 * {@code RedisPubSubMessenger}) to broadcast cache invalidation events across servers.
 * <p>
 * <strong>Active Invalidation:</strong> When a local cache entry is modified or deleted,
 * the manager broadcasts an invalidation event to all other servers.
 * <p>
 * <strong>Passive Invalidation:</strong> The manager listens for invalidation events from
 * other servers and notifies local cache consumers to invalidate their entries.
 * <p>
 * <strong>Cache Statistics:</strong> Tracks hits, misses, and invalidations for monitoring.
 *
 */
public class DistributedCacheManager implements AutoCloseable {

    /**
     * Channel name for cache invalidation messages.
     */
    public static final String CACHE_INVALIDATION_CHANNEL = "rapunzellib:cache:invalidate";

    public static final NetworkTopic<CacheInvalidationMessage> INVALIDATION_TOPIC =
        NetworkTopic.of(CACHE_INVALIDATION_CHANNEL, CacheInvalidationMessage.class);

    private final NetworkRuntimeGateway gateway;
    private final Logger logger;

    // Local cache consumers that will be notified of remote invalidations
    private final ConcurrentHashMap<String, Consumer<CacheInvalidationMessage>> invalidationListeners;

    // Statistics tracking
    private final CacheStatistics statistics;

    private final NetworkRuntimeGateway.Subscription invalidationSubscription;

    // Flag to track if this manager is active
    private volatile boolean active;

    /**
     * Creates a new DistributedCacheManager with the specified messenger.
     *
     * @param messenger the messenger to use for pub/sub communication
     */
    public DistributedCacheManager(@NotNull Messenger messenger) {
        this(messenger, LoggerFactory.getLogger(DistributedCacheManager.class));
    }

    /**
     * Creates a new DistributedCacheManager with the specified gateway.
     *
     * @param gateway the network runtime gateway
     */
    public DistributedCacheManager(@NotNull NetworkRuntimeGateway gateway) {
        this(gateway, LoggerFactory.getLogger(DistributedCacheManager.class));
    }

    /**
     * Creates a new DistributedCacheManager with the specified messenger and logger.
     *
     * @param messenger the messenger to use for pub/sub communication
     * @param logger    the logger to use for logging
     */
    public DistributedCacheManager(@NotNull Messenger messenger, @NotNull Logger logger) {
        this(DefaultNetworkRuntimeGateway.compatibility(messenger), logger);
    }

    /**
     * Creates a new DistributedCacheManager with the specified gateway and logger.
     *
     * @param gateway the network runtime gateway
     * @param logger  the logger
     */
    public DistributedCacheManager(@NotNull NetworkRuntimeGateway gateway, @NotNull Logger logger) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.invalidationListeners = new ConcurrentHashMap<>();
        this.statistics = new CacheStatistics();
        this.active = true;

        this.invalidationSubscription = gateway.subscribe(INVALIDATION_TOPIC, this::handleInvalidation);

        logger.debug("DistributedCacheManager initialized on server '{}'", gateway.runtime().localName());
    }

    /**
     * Broadcasts a cache invalidation event to all other servers.
     * <p>
     * This is the "active invalidation" side - when local cache is modified,
     * we tell other servers to invalidate their copies.
     *
     * @param entityClassName the fully qualified class name of the entity
     * @param id              the entity ID
     * @param operation       the operation type (UPSERT or DELETE)
     */
    public void broadcastInvalidation(@NotNull String entityClassName, @NotNull String id,
            @NotNull InvalidationOperation operation) {
        if (!active) {
            logger.warn("Cannot broadcast invalidation: DistributedCacheManager is not active");
            return;
        }

        Objects.requireNonNull(entityClassName, "entityClassName");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(operation, "operation");

        CacheInvalidationMessage message = new CacheInvalidationMessage(
                entityClassName,
                id,
                operation,
                gateway.runtime().localName(),
                System.currentTimeMillis()
        );

        try {
            gateway.publishToAll(INVALIDATION_TOPIC, message);
            statistics.incrementInvalidationsSent();
            logger.debug("Broadcast cache invalidation: {}#{} ({})", entityClassName, id, operation);
        } catch (Exception e) {
            logger.warn("Failed to broadcast cache invalidation for {}#{}", entityClassName, id, e);
        }
    }

    /**
     * Broadcasts a cache invalidation event using a CacheKey.
     *
     * @param key       the cache key
     * @param operation the operation type
     */
    public void broadcastInvalidation(@NotNull CacheKey key, @NotNull InvalidationOperation operation) {
        broadcastInvalidation(key.entityClassName(), key.id(), operation);
    }

    /**
     * Registers a listener to be notified when a remote invalidation is received.
     * <p>
     * This is the "passive invalidation" side - we listen for other servers'
     * invalidation events and notify local cache consumers.
     *
     * @param entityClassName the entity class name to listen for, or "*" for all
     * @param listener        the listener to notify
     */
    public void registerInvalidationListener(@NotNull String entityClassName,
            @NotNull Consumer<CacheInvalidationMessage> listener) {
        Objects.requireNonNull(entityClassName, "entityClassName");
        Objects.requireNonNull(listener, "listener");

        invalidationListeners.put(entityClassName, listener);
        logger.debug("Registered invalidation listener for entity: {}", entityClassName);
    }

    /**
     * Unregisters an invalidation listener.
     *
     * @param entityClassName the entity class name
     */
    public void unregisterInvalidationListener(@NotNull String entityClassName) {
        invalidationListeners.remove(entityClassName);
        logger.debug("Unregistered invalidation listener for entity: {}", entityClassName);
    }

    /**
     * Records a local cache hit.
     */
    public void recordHit() {
        statistics.incrementHits();
    }

    /**
     * Records a local cache miss.
     */
    public void recordMiss() {
        statistics.incrementMisses();
    }

    /**
     * Records a local cache invalidation (from remote).
     */
    public void recordInvalidationReceived() {
        statistics.incrementInvalidationsReceived();
    }

    /**
     * Closes the cache manager and unregisters the message listener.
     */
    @Override
    public void close() {
        if (active) {
            this.invalidationSubscription.close();
            this.active = false;
            logger.debug("DistributedCacheManager closed");
        }
    }

    /**
     * Checks if the cache manager is active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Handles an incoming cache invalidation message from another server.
     */
    private void handleInvalidation(CacheInvalidationMessage invalidationMessage, String sourceServer) {
        if (invalidationMessage == null) {
            return;
        }

        invalidationListeners.forEach((entityClassName, listener) -> {
            if (entityClassName.equals("*") || entityClassName.equals(invalidationMessage.entityClassName())) {
                try {
                    listener.accept(invalidationMessage);
                } catch (Exception e) {
                    logger.error("Error notifying listener for entity {}", entityClassName, e);
                }
            }
        });

        statistics.incrementInvalidationsReceived();
        logger.debug(
            "Received cache invalidation from server {}: {}#{} ({})",
            sourceServer,
            invalidationMessage.entityClassName(),
            invalidationMessage.id(),
            invalidationMessage.operation()
        );
    }
}
