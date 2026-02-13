package de.t14d3.rapunzellib.platform.velocity.network;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.t14d3.rapunzellib.network.InMemoryMessenger;
import de.t14d3.rapunzellib.network.info.NetworkInfoRpc;
import de.t14d3.rapunzellib.network.info.NetworkPlayerInfo;
import de.t14d3.rapunzellib.network.runtime.NetworkLink;
import de.t14d3.rapunzellib.network.runtime.NetworkLinkKind;
import de.t14d3.rapunzellib.network.runtime.NetworkNodeRole;
import de.t14d3.rapunzellib.network.runtime.NetworkPath;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntime;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkTopic;
import de.t14d3.rapunzellib.network.runtime.RpcMethod;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VelocityNetworkInfoAdaptersTest {
    @Test
    void serviceMapsServersAndPlayersFromProxy() {
        UUID alphaId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID betaId = UUID.fromString("00000000-0000-0000-0000-000000000102");
        ProxyServer proxy = proxy(
            List.of(server("hub"), server("minigames")),
            List.of(player(alphaId, "Alpha", "hub"), player(betaId, "Beta", null))
        );

        VelocityNetworkInfoService service = new VelocityNetworkInfoService(runtime("velocity"), proxy);

        assertEquals("velocity", service.networkServerName().join());
        assertEquals(List.of("hub", "minigames"), service.servers().join());
        assertEquals(
            List.of(
                new NetworkPlayerInfo(alphaId, "Alpha", "hub"),
                new NetworkPlayerInfo(betaId, "Beta", null)
            ),
            service.players().join()
        );
    }

    @Test
    void responderRegistersAndClosesAllRpcHandlers() {
        FakeGateway gateway = new FakeGateway();
        ProxyServer proxy = proxy(
            List.of(server("hub"), server("minigames")),
            List.of(player(UUID.fromString("00000000-0000-0000-0000-000000000103"), "Gamma", "hub"))
        );

        try (VelocityNetworkInfoResponder responder = new VelocityNetworkInfoResponder(gateway, proxy, LoggerFactory.getLogger(getClass()))) {
            assertTrue(gateway.handlers.containsKey(NetworkInfoRpc.WHO_AM_I_METHOD.serviceMethod()));
            assertTrue(gateway.handlers.containsKey(NetworkInfoRpc.LIST_SERVERS_METHOD.serviceMethod()));
            assertTrue(gateway.handlers.containsKey(NetworkInfoRpc.LIST_PLAYERS_METHOD.serviceMethod()));

            assertEquals("backend-1", gateway.invoke(NetworkInfoRpc.WHO_AM_I_METHOD, "backend-1").join());
            assertEquals(List.of("hub", "minigames"), gateway.invoke(NetworkInfoRpc.LIST_SERVERS_METHOD, "backend-1").join());
            assertEquals("hub", gateway.invoke(NetworkInfoRpc.LIST_PLAYERS_METHOD, "backend-1").join().getFirst().serverName());
        }

        assertEquals(3, gateway.closedSubscriptions.get());
    }

    private static ProxyServer proxy(Collection<RegisteredServer> servers, Collection<Player> players) {
        return (ProxyServer) Proxy.newProxyInstance(
            ProxyServer.class.getClassLoader(),
            new Class<?>[]{ProxyServer.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getAllServers" -> servers;
                case "getAllPlayers" -> players;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "velocity-proxy";
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static RegisteredServer server(String name) {
        ServerInfo info = new ServerInfo(name, InetSocketAddress.createUnresolved("127.0.0.1", 25565));
        return (RegisteredServer) Proxy.newProxyInstance(
            RegisteredServer.class.getClassLoader(),
            new Class<?>[]{RegisteredServer.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getServerInfo" -> info;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "registered-server-" + name;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Player player(UUID uuid, String name, String currentServerName) {
        Optional<ServerConnection> currentServer = currentServerName != null
            ? Optional.of((ServerConnection) Proxy.newProxyInstance(
                ServerConnection.class.getClassLoader(),
                new Class<?>[]{ServerConnection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getServerInfo" -> new ServerInfo(
                        currentServerName,
                        InetSocketAddress.createUnresolved("127.0.0.1", 25565)
                    );
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "server-connection-" + currentServerName;
                    default -> defaultValue(method.getReturnType());
                }
            ))
            : Optional.empty();
        return (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[]{Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> uuid;
                case "getUsername" -> name;
                case "getCurrentServer" -> currentServer;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "velocity-player-" + name;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static NetworkRuntime runtime(String localName) {
        InMemoryMessenger messenger = new InMemoryMessenger(localName, "velocity");
        return new NetworkRuntime() {
            @Override
            public @NotNull NetworkNodeRole localRole() {
                return NetworkNodeRole.PROXY;
            }

            @Override
            public @NotNull String localName() {
                return localName;
            }

            @Override
            public @NotNull String proxyName() {
                return "velocity";
            }

            @Override
            public @NotNull NetworkLink canonicalLink() {
                return new NetworkLink(NetworkLinkKind.IN_MEMORY, messenger);
            }

            @Override
            public @NotNull Optional<NetworkLink> bootstrapLink() {
                return Optional.empty();
            }

            @Override
            public @NotNull InMemoryMessenger messenger() {
                return messenger;
            }
        };
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class FakeGateway implements NetworkRuntimeGateway {
        private final Map<String, RpcHandler<?, ?>> handlers = new LinkedHashMap<>();
        private final AtomicInteger closedSubscriptions = new AtomicInteger();

        @Override
        public @NotNull NetworkRuntime runtime() {
            return VelocityNetworkInfoAdaptersTest.runtime("velocity");
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public <T> void publish(@NotNull NetworkTopic<T> topic, @NotNull NetworkPath path, T payload) {
        }

        @Override
        public <T> @NotNull Subscription subscribe(@NotNull NetworkTopic<T> topic, @NotNull TopicListener<T> listener) {
            return () -> { };
        }

        @Override
        public <Req, Res> @NotNull CompletableFuture<Res> call(
            @NotNull NetworkPath path,
            @NotNull RpcMethod<Req, Res> method,
            Req payload,
            Duration timeout
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <Req, Res> @NotNull Subscription register(
            @NotNull RpcMethod<Req, Res> method,
            @NotNull RpcHandler<Req, Res> handler
        ) {
            handlers.put(method.serviceMethod(), handler);
            return closedSubscriptions::incrementAndGet;
        }

        @Override
        public void close() {
        }

        @SuppressWarnings("unchecked")
        private <Req, Res> CompletableFuture<Res> invoke(RpcMethod<Req, Res> method, String sourceServer) {
            RpcHandler<Req, Res> handler = (RpcHandler<Req, Res>) handlers.get(method.serviceMethod());
            if (handler == null) {
                throw new AssertionError("Missing handler for " + method.serviceMethod());
            }
            return handler.handle(null, sourceServer);
        }
    }
}
