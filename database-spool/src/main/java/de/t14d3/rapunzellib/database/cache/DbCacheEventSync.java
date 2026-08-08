package de.t14d3.rapunzellib.database.cache;

import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.cache.CacheInvalidationMessage;
import de.t14d3.rapunzellib.network.cache.DistributedCacheManager;
import de.t14d3.rapunzellib.network.cache.InvalidationOperation;
import de.t14d3.rapunzellib.network.queue.NetworkOutboxMessage;
import de.t14d3.spool.cache.CacheEvent;
import de.t14d3.spool.cache.CacheEventSink;
import de.t14d3.spool.cache.CacheKey;
import de.t14d3.spool.cache.CacheProvider;
import de.t14d3.spool.cache.LocalMemoryCacheProvider;
import de.t14d3.spool.core.EntityManager;
import de.t14d3.spool.mapping.EntityMetadata;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wires a spool {@link EntityManager} into the RLib distributed cache
 * invalidation bus so entity cache events stay in sync across servers.
 *
 * <p>This is the RLib-native replacement for consumer-side hand-wired cache
 * sync: it serves as the {@link CacheEventSink} of the (shared) entity manager
 * and broadcasts every local cache event over
 * {@link DistributedCacheManager#CACHE_INVALIDATION_CHANNEL} via the
 * {@link DistributedCacheManager}; on the receiving side it invalidates the
 * local L2 cache, evicts the matching identity-map entry (so the next read is
 * a fresh database read - no manual {@code refresh()} hacks needed), and
 * notifies registered {@link Listener}s.</p>
 *
 * <p>Installing this on the shared entity manager enables the local L2 cache
 * ({@link LocalMemoryCacheProvider}) with the given TTL, mirroring the
 * behavior consumers previously configured by hand.</p>
 */
public final class DbCacheEventSync implements CacheEventSink, AutoCloseable {

    /**
     * Listener notified of remote cache events.
     *
     * @see #register(Listener)
     */
    public interface Listener {
        void onCacheEvent(CacheEvent event, String sourceServer);
    }

    private final DistributedCacheManager cacheManager;
    private final CacheProvider cacheProvider;
    private final EntityManager entityManager;
    private final Messenger localMessenger;
    private final Logger logger;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates a sync and wires it into the given entity manager.
     *
     * @param messenger     the messenger used for cross-server broadcast
     *                      (typically the consumer's effective messenger)
     * @param entityManager the shared entity manager to wire (mutated:
     *                      cache provider, TTL and event sink are installed)
     */
    public DbCacheEventSync(@NotNull Messenger messenger, @NotNull EntityManager entityManager) {
        this(messenger, entityManager, Duration.ofMinutes(10));
    }

    /**
     * Creates a sync with a custom local cache TTL.
     *
     * @param messenger     the messenger used for cross-server broadcast
     * @param entityManager the shared entity manager to wire (mutated)
     * @param cacheTtl      TTL of the local L2 cache
     */
    public DbCacheEventSync(
        @NotNull Messenger messenger,
        @NotNull EntityManager entityManager,
        @NotNull Duration cacheTtl
    ) {
        this.localMessenger = Objects.requireNonNull(messenger, "messenger");
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.logger = LoggerFactory.getLogger(DbCacheEventSync.class);
        this.cacheProvider = new LocalMemoryCacheProvider();
        this.entityManager
            .withCacheProvider(cacheProvider)
            .withCacheTtl(Objects.requireNonNull(cacheTtl, "cacheTtl"))
            .withCacheEventSink(this);
        this.cacheManager = new DistributedCacheManager(messenger, logger);
        this.cacheManager.registerInvalidationListener("*", this::onRemoteInvalidation);
    }

    /**
     * {@link CacheEventSink} entry point: broadcasts a local cache event to
     * the other servers. Spool already invalidated the local L2 cache before
     * invoking this sink.
     */
    @Override
    public void append(CacheEvent event) {
        if (event == null || event.key() == null) return;
        if (NetworkOutboxMessage.class.getName().equals(event.key().entityClassName())) {
            // Avoid infinite recursion: broadcasting a cache event enqueues a
            // network outbox row, which itself emits a cache event.
            return;
        }
        InvalidationOperation operation = event.operation() == CacheEvent.Operation.DELETE
            ? InvalidationOperation.DELETE
            : InvalidationOperation.UPSERT;
        cacheManager.broadcastInvalidation(event.key().entityClassName(), event.key().id(), operation);
    }

    /**
     * Registers a listener for remote cache events.
     *
     * @param listener the listener to register
     */
    public void register(@NotNull Listener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
    }

    /**
     * Unregisters a listener.
     *
     * @param listener the listener to unregister
     */
    public void unregister(@NotNull Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * The local cache provider installed on the entity manager.
     *
     * @return the local L2 cache provider
     */
    public @NotNull CacheProvider cacheProvider() {
        return cacheProvider;
    }

    private void onRemoteInvalidation(CacheInvalidationMessage message) {
        if (message == null) return;

        // Never invalidate our own writes (they are already fresh locally).
        String local = localMessenger.getServerName();
        if (message.serverName() != null && local != null && !local.isBlank()
            && !"unknown".equalsIgnoreCase(local)
            && message.serverName().equalsIgnoreCase(local)) {
            return;
        }

        CacheKey key = new CacheKey(message.entityClassName(), message.id());
        cacheProvider.invalidate(key);
        boolean deleted = message.operation() == InvalidationOperation.DELETE;
        evictIdentityMapEntry(message.entityClassName(), message.id(), deleted);

        CacheEvent event = new CacheEvent(
            message.operation() == InvalidationOperation.DELETE
                ? CacheEvent.Operation.DELETE
                : CacheEvent.Operation.UPSERT,
            key
        );
        dispatch(event, message.serverName());
    }

    /**
     * Refreshes (UPSERT) or detaches (DELETE) the stale identity-map entry so
     * the next read observes the committed remote state.
     *
     * <p>UPSERT events refresh the managed instance in place: the identity map
     * keeps its entry (subsequent {@code findAll()} calls return the same,
     * now-fresh instance) and a concurrently queued local save still flushes
     * (last local writer wins, matching the historical per-load refresh
     * semantics). DELETE events detach the entry so reads re-query and see the
     * row is gone.</p>
     */
    private void evictIdentityMapEntry(String entityClassName, String id, boolean deleted) {
        try {
            Class<?> entityClass = Class.forName(entityClassName);
            Object typedId = convertId(entityClass, id);
            if (typedId == null) return;
            if (!entityManager.isManaged(entityClass, typedId)) return;
            Object managed = entityManager.find(entityClass, typedId);
            if (managed == null) return;
            if (deleted) {
                entityManager.detach(managed);
            } else {
                entityManager.refresh(managed);
            }
        } catch (Exception e) {
            // The row may have been deleted concurrently - drop the entry so
            // the next read re-queries instead of serving a stale instance.
            try {
                Class<?> entityClass = Class.forName(entityClassName);
                Object typedId = convertId(entityClass, id);
                Object managed = typedId == null ? null : entityManager.find(entityClass, typedId);
                if (managed != null) {
                    entityManager.detach(managed);
                }
            } catch (Exception ignored) {
                // Nothing more to do.
            }
            logger.debug("Failed to refresh identity-map entry {}#{}; detached", entityClassName, id, e);
        }
    }

    /**
     * Converts the stringified id of a cache key back to the entity's id type
     * so the identity map can be addressed with a matching key.
     */
    private static Object convertId(Class<?> entityClass, String id) {
        try {
            Field idField = EntityMetadata.of(entityClass).getIdField();
            if (idField == null) return null;
            Class<?> type = idField.getType();
            if (type == long.class || type == Long.class) return Long.parseLong(id);
            if (type == int.class || type == Integer.class) return Integer.parseInt(id);
            if (type == short.class || type == Short.class) return Short.parseShort(id);
            if (type == java.util.UUID.class) return java.util.UUID.fromString(id);
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    private void dispatch(CacheEvent event, String sourceServer) {
        for (Listener listener : listeners) {
            try {
                listener.onCacheEvent(event, sourceServer);
            } catch (Exception ignored) {
                // A failing consumer listener must not break the sync bus.
            }
        }
    }

    @Override
    public void close() {
        cacheManager.close();
        try {
            cacheProvider.close();
        } catch (Exception ignored) {
        }
    }
}
