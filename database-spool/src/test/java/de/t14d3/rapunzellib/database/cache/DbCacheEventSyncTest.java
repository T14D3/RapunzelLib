package de.t14d3.rapunzellib.database.cache;

import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.cache.CacheEvent;
import de.t14d3.spool.cache.CacheKey;
import de.t14d3.spool.core.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end cross-server cache event sync via {@link DbCacheEventSync}:
 * a save on backend A must fire the listener on backend B and invalidate B's
 * local L2 cache and identity-map entry.
 */
final class DbCacheEventSyncTest {

    @Entity
    public static class SyncEntity {
        @Id(autoIncrement = true)
        @Column(name = "id")
        private long id;

        @Column(name = "value", type = "VARCHAR(64)")
        private String value;

        public long getId() {
            return id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /** Minimal in-memory network that delivers to ALL nodes including the sender. */
    private static final class TestNetwork {
        private final Map<String, Node> nodes = new ConcurrentHashMap<>();

        Messenger messenger(String serverName) {
            Node node = nodes.computeIfAbsent(serverName, _ignored -> new Node());
            return new Messenger() {
                @Override
                public void sendToAll(@NotNull String channel, @NotNull String data) {
                    for (Node target : nodes.values()) {
                        target.deliver(channel, data, serverName);
                    }
                }

                @Override
                public void sendToServer(@NotNull String channel, @NotNull String targetServerName, @NotNull String data) {
                    Node target = nodes.get(targetServerName);
                    if (target != null) {
                        target.deliver(channel, data, serverName);
                    }
                }

                @Override
                public void sendToProxy(@NotNull String channel, @NotNull String data) {
                    sendToServer(channel, "proxy", data);
                }

                @Override
                public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
                    node.listeners.computeIfAbsent(channel, _ignored -> new CopyOnWriteArrayList<>()).add(listener);
                }

                @Override
                public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
                    List<MessageListener> list = node.listeners.get(channel);
                    if (list != null) {
                        list.remove(listener);
                    }
                }

                @Override
                public boolean isConnected() {
                    return true;
                }

                @Override
                public @NotNull String getServerName() {
                    return serverName;
                }

                @Override
                public @NotNull String getProxyServerName() {
                    return "proxy";
                }
            };
        }

        private static final class Node {
            private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();

            private void deliver(String channel, String data, String sourceServer) {
                List<MessageListener> channelListeners = listeners.get(channel);
                if (channelListeners == null) {
                    return;
                }
                for (MessageListener listener : List.copyOf(channelListeners)) {
                    listener.onMessage(channel, data, sourceServer);
                }
            }
        }
    }

    @Test
    void saveOnAlphaFiresListenerOnBetaAndInvalidatesBetaCache() throws Exception {
        TestNetwork network = new TestNetwork();
        // Shared in-memory database: alpha's committed save must be visible to
        // beta's fresh re-read after the invalidation (read-through semantics).
        String jdbcUrl = "jdbc:h2:mem:dbcache;DB_CLOSE_DELAY=-1";
        EntityManager alphaEm = EntityManager.create(jdbcUrl);
        alphaEm.registerEntities(SyncEntity.class);
        alphaEm.updateSchema();

        EntityManager betaEm = EntityManager.create(jdbcUrl);
        betaEm.registerEntities(SyncEntity.class);
        betaEm.updateSchema();

        AtomicReference<CacheEvent> received = new AtomicReference<>();
        AtomicReference<String> receivedSource = new AtomicReference<>();

        try (
            DbCacheEventSync alphaSync = new DbCacheEventSync(network.messenger("alpha"), alphaEm);
            DbCacheEventSync betaSync = new DbCacheEventSync(network.messenger("beta"), betaEm)
        ) {
            betaSync.register((event, sourceServer) -> {
                received.set(event);
                receivedSource.set(sourceServer);
            });

            // ── Alpha writes the initial row ────────────────────────────────
            SyncEntity alphaEntity = new SyncEntity();
            alphaEntity.setValue("v1");
            alphaEm.persist(alphaEntity);
            alphaEm.flush();

            // ── Beta loads the row (stale read before alpha's update) ───────
            SyncEntity betaEntity = betaEm.find(SyncEntity.class, 1L);
            assertNotNull(betaEntity);
            CacheKey key = new CacheKey(SyncEntity.class.getName(), "1");
            assertTrue(betaSync.cacheProvider().get(key).isPresent(),
                "beta L2 cache should hold the loaded entity");

            // ── Alpha updates (the remote writer) ───────────────────────────
            alphaEntity.setValue("fresh");
            alphaEm.persist(alphaEntity); // managed entity -> queued update
            alphaEm.flush();

            // The save's cache event must reach beta's listener with the
            // writer's server name.
            CacheEvent event = received.get();
            assertNotNull(event, "beta listener should have fired");
            assertEquals(SyncEntity.class.getName(), event.key().entityClassName());
            assertEquals("1", event.key().id());
            assertEquals("alpha", receivedSource.get());

            // Beta's L2 cache entry must no longer hold the stale snapshot
            // (the refresh re-populates it with the committed state).
            var l2 = betaSync.cacheProvider().get(key);
            assertTrue(l2.isPresent(), "beta L2 cache should hold the refreshed entity");
            assertEquals("fresh", l2.get().fieldValues().get("value"),
                "beta L2 cache must not serve the stale snapshot; actual=" + l2.get().fieldValues());

            // Beta's identity-map entry must be refreshed in place: a re-read
            // returns the same managed instance but observes alpha's committed
            // save (read-through semantics, no manual refresh() needed).
            SyncEntity reRead = betaEm.find(SyncEntity.class, 1L);
            assertNotNull(reRead);
            assertSame(betaEntity, reRead, "beta identity map keeps its managed instance");
            assertEquals("fresh", reRead.getValue(),
                "beta re-read must observe alpha's committed save");
        }
    }

    @Test
    void selfEventsDoNotNotifyLocalListener() throws Exception {
        TestNetwork network = new TestNetwork();
        EntityManager alphaEm = EntityManager.create("jdbc:h2:mem:dbcacheself;DB_CLOSE_DELAY=-1");
        alphaEm.registerEntities(SyncEntity.class);
        alphaEm.updateSchema();

        AtomicReference<CacheEvent> received = new AtomicReference<>();
        try (DbCacheEventSync alphaSync = new DbCacheEventSync(network.messenger("alpha"), alphaEm)) {
            alphaSync.register((event, sourceServer) -> received.set(event));

            SyncEntity entity = new SyncEntity();
            entity.setValue("local");
            alphaEm.persist(entity);
            alphaEm.flush();

            // The self-broadcast (in-memory networks deliver to the sender too)
            // must be filtered out by the origin-server check.
            assertEquals(null, received.get(), "local save must not notify local listener");
        }
    }
}
