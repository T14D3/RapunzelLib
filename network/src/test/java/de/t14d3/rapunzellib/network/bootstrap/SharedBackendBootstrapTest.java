package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntime;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the backend bootstrap binds the locally resolved server name to the
 * plugin messenger immediately, instead of waiting for (and depending on) the proxy's
 * NetworkInfo RPC round-trip.
 */
final class SharedBackendBootstrapTest {

    @TempDir
    Path tempDir;

    @Test
    void appliesResolvedConfigServerNameToPluginMessengerAtBootstrap() throws Exception {
        Files.writeString(tempDir.resolve("config.yml"), """
                network:
                  transport: plugin
                  serverName: lobby
                  proxyServerName: velocity
                """);
        NamedMessenger pluginMessenger = new NamedMessenger();

        RapunzelContext context = SharedBackendBootstrap.createContext(
            "test-lease",
            "test",
            "test",
            runtime(),
            LoggerFactory.getLogger(SharedBackendBootstrapTest.class),
            tempDir,
            path -> java.util.Optional.empty(),
            new StubScheduler(),
            ctx -> {
            },
            new BackendTransportBootstrap.Hooks(
                BackendTransportBootstrap.PluginHooks.standard(() -> pluginMessenger)
            ),
            NamedMessenger.class,
            NamedMessenger::setNetworkServerName
        );

        assertEquals("lobby", pluginMessenger.getServerName(),
            "plugin messenger must know its name immediately from config, not only after an RPC round-trip");
        assertEquals("lobby", context.services().find(NetworkRuntime.class).orElseThrow().localName(),
            "network runtime local name must reflect the bound plugin messenger name");
    }

    @Test
    void leavesPluginMessengerUnboundWhenConfigHasNoServerName() throws Exception {
        Files.writeString(tempDir.resolve("config.yml"), """
                network:
                  transport: plugin
                """);
        NamedMessenger pluginMessenger = new NamedMessenger();

        SharedBackendBootstrap.createContext(
            "test-lease",
            "test",
            "test",
            runtime(),
            LoggerFactory.getLogger(SharedBackendBootstrapTest.class),
            tempDir,
            path -> java.util.Optional.empty(),
            new StubScheduler(),
            ctx -> {
            },
            new BackendTransportBootstrap.Hooks(
                BackendTransportBootstrap.PluginHooks.standard(() -> pluginMessenger)
            ),
            NamedMessenger.class,
            NamedMessenger::setNetworkServerName
        );

        assertEquals("unknown", pluginMessenger.getServerName(),
            "no name may be bound when the resolved server name is blank");
    }

    private static PlatformRuntime runtime() {
        return new PlatformRuntime(
            PlatformId.PAPER,
            RuntimeRole.SERVER,
            EngineFamily.MOJANG_SERVER,
            Set.of(),
            new LifecycleOwner(SharedBackendBootstrapTest.class)
        );
    }

    /** Messenger whose name is only set through the same hook platforms pass in. */
    private static final class NamedMessenger implements Messenger {
        private volatile String networkServerName;

        void setNetworkServerName(String networkServerName) {
            if (networkServerName == null || networkServerName.isBlank()) return;
            this.networkServerName = networkServerName;
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
            String current = networkServerName;
            return current != null && !current.isBlank() ? current : "unknown";
        }

        @Override
        public @NotNull String getProxyServerName() {
            return "velocity";
        }
    }

    private static final class StubScheduler implements Scheduler {
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
        public @NotNull ScheduledTask runRepeating(
            @NotNull Duration initialDelay,
            @NotNull Duration period,
            @NotNull Runnable task
        ) {
            return taskHandle();
        }

        @Override
        public @NotNull ScheduledTask runRepeatingAsync(
            @NotNull Duration initialDelay,
            @NotNull Duration period,
            @NotNull Runnable task
        ) {
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
    }
}
