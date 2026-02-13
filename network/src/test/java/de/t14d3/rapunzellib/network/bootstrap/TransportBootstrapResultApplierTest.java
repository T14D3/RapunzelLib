package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.runtime.NetworkLinkKind;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntime;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

final class TransportBootstrapResultApplierTest {
    @Test
    void registersTypedNetworkRuntimeAlongsideCompatibilityMessenger() {
        TestContext context = new TestContext();
        Messenger plugin = new StubMessenger("velocity-main", "velocity-main");
        Messenger rpc = new StubMessenger("velocity-main", "velocity-main");

        Messenger effective = TransportBootstrapResultApplier.apply(
            context,
            LoggerFactory.getLogger(TransportBootstrapResultApplierTest.class),
            new BackendTransportBootstrap.Result(
                PlatformId.VELOCITY,
                "velocity-main",
                "velocity-main",
                MessengerTransportBootstrap.TransportPriority.PLUGIN_FIRST,
                plugin,
                plugin,
                null,
                rpc,
                rpc,
                null,
                null,
                null
            )
        );

        NetworkRuntime runtime = context.services().find(NetworkRuntime.class).orElseThrow();
        NetworkRuntimeGateway gateway = context.services().find(NetworkRuntimeGateway.class).orElseThrow();
        assertSame(rpc, effective);
        assertSame(rpc, context.services().find(Messenger.class).orElseThrow());
        assertSame(runtime, gateway.runtime());
        assertEquals(NetworkLinkKind.RPC, runtime.canonicalLink().kind());
        assertEquals(NetworkLinkKind.PLUGIN_MESSAGING, runtime.bootstrapLink().orElseThrow().kind());
        assertSame(plugin, runtime.bootstrapMessenger().orElseThrow());
        assertInstanceOf(StubMessenger.class, runtime.canonicalMessenger());
        assertSame(rpc, runtime.canonicalMessenger());
    }

    private static final class TestContext implements RapunzelContext {
        private final Logger logger = LoggerFactory.getLogger(TransportBootstrapResultApplierTest.class);
        private final ServiceRegistry services = new TestServiceRegistry();
        private final PlatformRuntime runtime = new PlatformRuntime(
            PlatformId.VELOCITY,
            RuntimeRole.PROXY,
            EngineFamily.PROXY,
            java.util.Set.of(),
            new LifecycleOwner(this)
        );

        @Override
        public @NotNull PlatformRuntime runtime() {
            return runtime;
        }

        @Override
        public @NotNull Logger logger() {
            return logger;
        }

        @Override
        public @NotNull Path dataDirectory() {
            return Path.of(".");
        }

        @Override
        public @NotNull ResourceProvider resources() {
            return path -> Optional.empty();
        }

        @Override
        public @NotNull Scheduler scheduler() {
            return new Scheduler() {
                @Override
                public @NotNull ScheduledTask run(@NotNull Runnable task) {
                    return taskHandle();
                }

                @Override
                public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
                    return taskHandle();
                }

                @Override
                public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
                    return taskHandle();
                }

                @Override
                public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
                    return taskHandle();
                }

                @Override
                public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
                    return taskHandle();
                }

                private ScheduledTask taskHandle() {
                    return new ScheduledTask() {
                        @Override
                        public void cancel() {
                        }

                        @Override
                        public boolean isCancelled() {
                            return false;
                        }
                    };
                }
            };
        }

        @Override
        public @NotNull ServiceRegistry services() {
            return services;
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

    private static final class StubMessenger implements Messenger {
        private final String serverName;
        private final String proxyName;

        private StubMessenger(String serverName, String proxyName) {
            this.serverName = serverName;
            this.proxyName = proxyName;
        }

        @Override
        public void sendToAll(@NotNull String channel, @NotNull String data) {
        }

        @Override
        public void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data) {
        }

        @Override
        public void sendToProxy(@NotNull String channel, @NotNull String data) {
        }

        @Override
        public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
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
            return serverName;
        }

        @Override
        public @NotNull String getProxyServerName() {
            return proxyName;
        }
    }
}
