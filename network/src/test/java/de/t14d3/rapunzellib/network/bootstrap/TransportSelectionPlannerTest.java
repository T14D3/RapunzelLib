package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.network.CompositeMessenger;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkHealthMonitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class TransportSelectionPlannerTest {
    @Test
    void pluginOnlyUsesPluginOrFallsBackToInMemory() {
        TestServiceRegistry services = new TestServiceRegistry();
        Messenger inMemory = new StubMessenger("in-memory");
        Messenger plugin = new StubMessenger("plugin");

        Messenger selected = select(
            MessengerTransportBootstrap.TransportPriority.PLUGIN_ONLY,
            services,
            inMemory,
            plugin,
            null,
            null
        );
        assertEquals("plugin", selected.getServerName());

        Messenger selectedFallback = select(
            MessengerTransportBootstrap.TransportPriority.PLUGIN_ONLY,
            services,
            inMemory,
            null,
            null,
            null
        );
        assertSame(inMemory, selectedFallback);
    }

    @Test
    void redisOnlyUsesRedisOrFallsBackToInMemory() {
        TestServiceRegistry services = new TestServiceRegistry();
        Messenger inMemory = new StubMessenger("in-memory");
        Messenger redis = new StubMessenger("redis");

        Messenger selected = select(
            MessengerTransportBootstrap.TransportPriority.REDIS_ONLY,
            services,
            inMemory,
            null,
            redis,
            null
        );
        assertEquals("redis", selected.getServerName());

        Messenger selectedFallback = select(
            MessengerTransportBootstrap.TransportPriority.REDIS_ONLY,
            services,
            inMemory,
            null,
            null,
            null
        );
        assertSame(inMemory, selectedFallback);
    }

    @Test
    void rpcServerOnlyUsesRpcOrFallsBackToInMemory() {
        TestServiceRegistry services = new TestServiceRegistry();
        Messenger inMemory = new StubMessenger("in-memory");
        Messenger rpc = new StubMessenger("rpc");

        Messenger selected = select(
            MessengerTransportBootstrap.TransportPriority.RPC_SERVER_ONLY,
            services,
            inMemory,
            null,
            null,
            rpc
        );
        assertEquals("rpc", selected.getServerName());

        Messenger selectedFallback = select(
            MessengerTransportBootstrap.TransportPriority.RPC_SERVER_ONLY,
            services,
            inMemory,
            null,
            null,
            null
        );
        assertSame(inMemory, selectedFallback);
    }


    @Test
    void pluginFirstBuildsCompositeWithHealthMonitor() {
        TestServiceRegistry services = new TestServiceRegistry();
        Messenger inMemory = new StubMessenger("in-memory");
        Messenger plugin = new StubMessenger("plugin");
        Messenger redis = new StubMessenger("redis");

        Messenger selected = select(
            MessengerTransportBootstrap.TransportPriority.PLUGIN_FIRST,
            services,
            inMemory,
            plugin,
            redis,
            null
        );

        assertInstanceOf(CompositeMessenger.class, selected);
        assertNotNull(services.find(NetworkHealthMonitor.class).orElse(null));
        assertEquals("plugin", selected.getServerName());
    }

    @Test
    void pluginFirstFallsBackToRedisThenRpcThenInMemory() {
        TestServiceRegistry services = new TestServiceRegistry();
        Messenger inMemory = new StubMessenger("in-memory");
        Messenger rpc = new StubMessenger("rpc");

        Messenger selected = select(
            MessengerTransportBootstrap.TransportPriority.PLUGIN_FIRST,
            services,
            inMemory,
            null,
            null,
            rpc
        );
        assertEquals("rpc", selected.getServerName());

        Messenger selectedFallback = select(
            MessengerTransportBootstrap.TransportPriority.PLUGIN_FIRST,
            services,
            inMemory,
            null,
            null,
            null
        );
        assertSame(inMemory, selectedFallback);
    }

    @Test
    void redisFirstFallsBackToPluginThenRpcThenInMemory() {
        TestServiceRegistry services = new TestServiceRegistry();
        Messenger inMemory = new StubMessenger("in-memory");
        Messenger plugin = new StubMessenger("plugin");
        Messenger rpc = new StubMessenger("rpc");

        Messenger selected = select(
            MessengerTransportBootstrap.TransportPriority.REDIS_FIRST,
            services,
            inMemory,
            plugin,
            null,
            rpc
        );
        assertEquals("plugin", selected.getServerName());

        Messenger selectedFallback = select(
            MessengerTransportBootstrap.TransportPriority.REDIS_FIRST,
            services,
            inMemory,
            null,
            null,
            null
        );
        assertSame(inMemory, selectedFallback);
    }

    @Test
    void rpcServerFirstFallsBackToRedisThenPluginThenInMemory() {
        TestServiceRegistry services = new TestServiceRegistry();
        Messenger inMemory = new StubMessenger("in-memory");
        Messenger redis = new StubMessenger("redis");
        Messenger plugin = new StubMessenger("plugin");

        Messenger selected = select(
            MessengerTransportBootstrap.TransportPriority.RPC_SERVER_FIRST,
            services,
            inMemory,
            plugin,
            redis,
            null
        );
        assertEquals("redis", selected.getServerName());

        Messenger selectedFallback = select(
            MessengerTransportBootstrap.TransportPriority.RPC_SERVER_FIRST,
            services,
            inMemory,
            null,
            null,
            null
        );
        assertSame(inMemory, selectedFallback);
    }



    @Test
    void pluginFirstDedupesDuplicateFallbackMessengerInstances() {
        TestServiceRegistry services = new TestServiceRegistry();
        StubMessenger inMemory = new StubMessenger("in-memory");
        StubMessenger plugin = new StubMessenger("plugin");
        StubMessenger sharedFallback = new StubMessenger("shared");

        Messenger selected = select(
            MessengerTransportBootstrap.TransportPriority.PLUGIN_FIRST,
            services,
            inMemory,
            plugin,
            sharedFallback,
            sharedFallback
        );

        assertInstanceOf(CompositeMessenger.class, selected);
        selected.registerListener("channel", (channel, data, senderServer) -> {
        });
        assertEquals(1, plugin.listenerRegistrations);
    }

    private static Messenger select(
        MessengerTransportBootstrap.TransportPriority priority,
        TestServiceRegistry services,
        Messenger inMemory,
        Messenger plugin,
        Messenger redis,
        Messenger rpc
    ) {
        return TransportSelectionPlanner.select(
            priority,
            LoggerFactory.getLogger(TransportSelectionPlannerTest.class),
            services,
            inMemory,
            plugin,
            redis,
            rpc
        );
    }

    private static final class StubMessenger implements Messenger {
        private final String id;
        private int sent;
        private int listenerRegistrations;

        private StubMessenger(String id) {
            this.id = id;
        }

        @Override
        public void sendToAll(@NotNull String channel, @NotNull String data) {
            sent++;
        }

        @Override
        public void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data) {
            sent++;
        }

        @Override
        public void sendToProxy(@NotNull String channel, @NotNull String data) {
            sent++;
        }

        @Override
        public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
            listenerRegistrations++;
        }

        @Override
        public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public @NotNull String getServerName() {
            return id;
        }

        @Override
        public @NotNull String getProxyServerName() {
            return "velocity";
        }
    }

    private static final class TestServiceRegistry implements ServiceRegistry {
        private final Map<Class<?>, Object> services = new LinkedHashMap<>();

        @Override
        public <T> void register(@NotNull Class<T> type, @NotNull T instance) {
            services.put(type, instance);
        }

        @Override
        public <T> @NotNull Optional<T> find(@NotNull Class<T> type) {
            Object value = services.get(type);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(type.cast(value));
        }

        @Override
        public @NotNull List<Class<?>> serviceTypes() {
            return new ArrayList<>(services.keySet());
        }

        @Override
        public @NotNull List<Object> services() {
            return new ArrayList<>(services.values());
        }
    }
}
