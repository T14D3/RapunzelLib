package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.info.NetworkInfoRpc;
import de.t14d3.rapunzellib.network.info.NetworkPlayerInfo;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import de.t14d3.rapunzellib.network.rpc.RpcClient;
import de.t14d3.rapunzellib.network.rpcserver.RpcServerConfig;
import de.t14d3.rapunzellib.network.rpcserver.RpcServerMessenger;
import de.t14d3.rapunzellib.network.rpcserver.RoutingHooks;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces the production plugin-transport topology where the platform's own
 * config carries no {@code network.serverName} (the consumer binds the name
 * AFTER the RLib bootstrap). Verifies that the backend's
 * {@code list_local_players} answer reports the player's server as the live
 * bound name instead of the bootstrap-time {@code "unknown"} snapshot.
 *
 * <p>Regression test for cross-server TPA: with the stale name, the lobby
 * resolves a remote player to server {@code "unknown"} and the proxy drops the
 * SERVER-targeted request envelope addressed to that name.</p>
 */
final class BackendNetworkInfoResponderTest {

    private static final UUID BOB_UUID = UUID.fromString("1a5a14e7-f8b4-304b-99e0-7a269d1e32ab");

    @TempDir
    Path tempDir;

    private RpcServerMessenger proxy;

    @AfterEach
    void tearDown() {
        if (proxy != null) {
            proxy.close();
        }
    }

    @Test
    void listLocalPlayersReportsLiveBoundServerNameInsteadOfUnknown() throws Exception {
        int proxyPort = freePort();
        proxy = new RpcServerMessenger(
            RpcServerConfig.builder("velocity").port(proxyPort).build(),
            LoggerFactory.getLogger("test-proxy"),
            new RoutingHooks(
                () -> List.of("lobby", "survival"),
                (channel, data, sourceServer, targetServer) -> false
            )
        );

        Files.writeString(tempDir.resolve("config.yml"), """
                network:
                  transport: plugin
                  rpcServer:
                    host: 127.0.0.1
                    port: %d
                """.formatted(proxyPort));

        LateBoundMessenger pluginMessenger = new LateBoundMessenger();
        Players stubPlayers = stubPlayers();

        RapunzelContext context = SharedBackendBootstrap.createContext(
            "test-lease",
            "test",
            "test",
            runtime(),
            LoggerFactory.getLogger(BackendNetworkInfoResponderTest.class),
            tempDir,
            path -> java.util.Optional.empty(),
            new StubScheduler(),
            ctx -> ctx.register(Players.class, stubPlayers),
            new BackendTransportBootstrap.Hooks(
                BackendTransportBootstrap.PluginHooks.standard(() -> pluginMessenger)
            ),
            LateBoundMessenger.class,
            LateBoundMessenger::setNetworkServerName
        );

        try {
            // The consumer binds the server name AFTER the RLib bootstrap, when
            // the runtime local name is already fixed to "unknown".
            pluginMessenger.setNetworkServerName("survival");

            // Trigger the TCP bridge link (it connects lazily on the first send
            // once a resolved name is available).
            Messenger bridge = context.services().get(Messenger.class);
            bridge.sendToProxy("test:warmup", "ping");

            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline && !proxy.isServerConnected("survival")) {
                Thread.sleep(50);
            }
            assertTrue(proxy.isServerConnected("survival"),
                "backend TCP bridge should connect and identify as 'survival'");

            // Act as the proxy's LIST_PLAYERS responder: query the backend for
            // its local players over the companion TCP bridge.
            NetworkRuntimeGateway proxyGateway = DefaultNetworkRuntimeGateway.compatibility(
                proxy, JsonCodecs.gson());
            RpcClient rpc = new RpcClient(
                proxyGateway, new StubScheduler(), LoggerFactory.getLogger("test-rpc-client"),
                Duration.ofSeconds(5), JsonCodecs.gson());
            try {
                List<NetworkPlayerInfo> players = rpc.callServer(
                    "survival", NetworkInfoRpc.LIST_LOCAL_PLAYERS_METHOD, null
                ).get(10, TimeUnit.SECONDS);

                assertNotNull(players, "backend should answer list_local_players");
                NetworkPlayerInfo bob = players.stream()
                    .filter(p -> BOB_UUID.equals(p.uuid()))
                    .findFirst()
                    .orElse(null);
                assertNotNull(bob, "backend should report its local player");
                assertEquals("survival", bob.serverName(),
                    "backend-local player must be reported under the LIVE bound server name, "
                        + "not the bootstrap-time 'unknown' snapshot (got " + bob.serverName() + ")");
            } finally {
                rpc.close();
            }
        } finally {
            context.close();
        }
    }

    private static Players stubPlayers() {
        RPlayer bob = (RPlayer) Proxy.newProxyInstance(
            RPlayer.class.getClassLoader(),
            new Class<?>[]{RPlayer.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "uuid" -> BOB_UUID;
                case "name" -> "BotBob";
                case "toString" -> "BotBob";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> method.getDefaultValue();
            });
        return new Players() {
            @Override
            public @NotNull java.util.Collection<RPlayer> online() {
                return List.of(bob);
            }

            @Override
            public @NotNull java.util.Optional<RPlayer> get(@NotNull UUID uuid) {
                return BOB_UUID.equals(uuid) ? java.util.Optional.of(bob) : java.util.Optional.empty();
            }

            @Override
            public @NotNull java.util.Optional<RPlayer> wrap(@NotNull Object nativePlayer) {
                return java.util.Optional.empty();
            }
        };
    }

    private static PlatformRuntime runtime() {
        return new PlatformRuntime(
            PlatformId.PAPER,
            RuntimeRole.SERVER,
            EngineFamily.MOJANG_SERVER,
            Set.of(),
            new LifecycleOwner(BackendNetworkInfoResponderTest.class)
        );
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * Messenger that starts as {@code "unknown"} and only learns its name
     * through the same hook platforms pass in - mirroring a consumer binding
     * the name after the RLib bootstrap.
     */
    private static final class LateBoundMessenger implements Messenger {
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
