package de.t14d3.rapunzellib.network.rpcserver;

import de.t14d3.rapunzellib.network.MessageListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test of the TCP companion bridge used by the plugin transport:
 * a proxy-side {@link RpcServerMessenger} with routing hooks plus two
 * backend-side {@link RpcClientMessenger} instances.
 *
 * <p>Verifies broadcast, proxy-addressed (RPC) envelopes, targeted sends and
 * local loopback - the exact flows the plugin transport needs when players
 * connect directly to backends (no plugin-message carrier through the proxy).</p>
 */
final class PluginTransportTcpBridgeTest {

    private int port;
    private RpcServerMessenger proxy;
    private RpcClientMessenger lobby;
    private RpcClientMessenger survival;

    private final List<String> proxyMessages = new CopyOnWriteArrayList<>();
    private final List<String> lobbyMessages = new CopyOnWriteArrayList<>();
    private final List<String> survivalMessages = new CopyOnWriteArrayList<>();
    private final List<String> externalForwards = new CopyOnWriteArrayList<>();
    private final AtomicBoolean proxyAddressedDelivered = new AtomicBoolean(false);

    @BeforeEach
    void setUp() throws Exception {
        port = freePort();
        RoutingHooks hooks = new RoutingHooks(
            () -> List.of("lobby", "survival"),
            (channel, data, sourceServer, targetServer) -> {
                externalForwards.add(channel + "|" + targetServer);
                return false; // no plugin channel in this test
            }
        );
        proxy = new RpcServerMessenger(
            RpcServerConfig.builder("velocity").port(port).build(),
            LoggerFactory.getLogger("test-proxy"),
            hooks
        );
        // Proxy-local listeners (as if the velocity gateway subscribed).
        proxy.registerListener("chat", capture(proxyMessages, "chat"));
        proxy.registerListener("rpc", capture(proxyMessages, "rpc"));

        lobby = new RpcClientMessenger(new RpcClientConfig("127.0.0.1", port, "lobby"), LoggerFactory.getLogger("test-lobby"));
        survival = new RpcClientMessenger(new RpcClientConfig("127.0.0.1", port, "survival"), LoggerFactory.getLogger("test-survival"));
        lobby.registerListener("chat", capture(lobbyMessages, "chat"));
        lobby.registerListener("rpc", capture(lobbyMessages, "rpc"));
        survival.registerListener("chat", capture(survivalMessages, "chat"));
        survival.registerListener("rpc", capture(survivalMessages, "rpc"));
        // Proxy-addressed detection on the survival client: register a latch on "rpc".
        CountDownLatch latch = new CountDownLatch(1);
        survival.registerListener("rpc", (channel, data, source) -> latch.countDown());
        this.proxyAddressedDelivered.set(false);
        this.survivalRpcLatch = latch;

        awaitIdentified();
    }

    private CountDownLatch survivalRpcLatch;

    @AfterEach
    void tearDown() {
        closeQuietly(lobby);
        closeQuietly(survival);
        closeQuietly(proxy);
    }

    private void awaitIdentified() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (lobby.isIdentified() && survival.isIdentified()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("Backend clients did not identify in time");
    }

    @Test
    void broadcastDeliversToProxyAndOtherBackendAndSenderLocally() throws Exception {
        lobby.sendToAll("chat", "hello");

        Thread.sleep(500);
        assertTrue(contains(lobbyMessages, "chat|hello|lobby"), "sender should receive its own broadcast locally, got " + lobbyMessages);
        assertTrue(contains(survivalMessages, "chat|hello|lobby"), "other backend should receive the broadcast, got " + survivalMessages);
        assertTrue(contains(proxyMessages, "chat|hello|lobby"), "proxy should receive the broadcast, got " + proxyMessages);
        assertTrue(externalForwards.isEmpty(), "no external forward expected when both backends are TCP-connected, got " + externalForwards);
    }

    @Test
    void proxyAddressedEnvelopeIsNeverForwardedToBackends() throws Exception {
        lobby.sendToProxy("rpc", "request-1");

        assertFalse(survivalRpcLatch.await(1_000, TimeUnit.MILLISECONDS),
            "proxy-addressed envelope must not reach other backends");
        assertTrue(contains(proxyMessages, "rpc|request-1|lobby"), "proxy should receive the proxy-addressed envelope, got " + proxyMessages);
    }

    @Test
    void targetedSendReachesOnlyTheTargetBackend() throws Exception {
        lobby.sendToServer("chat", "survival", "to-survival");
        lobby.sendToServer("chat", "lobby", "to-self");

        Thread.sleep(500);
        assertTrue(contains(survivalMessages, "chat|to-survival|lobby"), "target backend should receive the targeted send, got " + survivalMessages);
        assertFalse(contains(lobbyMessages, "chat|to-survival|lobby"), "non-target backend must not receive the targeted send, got " + lobbyMessages);
        assertTrue(contains(lobbyMessages, "chat|to-self|lobby"), "self-targeted send should loop back locally, got " + lobbyMessages);
    }

    @Test
    void knownBackendWithoutTcpConnectionIsRoutedThroughExternalForward() throws Exception {
        // A broadcast from survival while "survival" is also the source: lobby is connected,
        // but pretend a third backend "minigames" is known yet not TCP-connected.
        RoutingHooks hooks = new RoutingHooks(
            () -> List.of("lobby", "minigames"),
            (channel, data, sourceServer, targetServer) -> {
                externalForwards.add(channel + "|" + targetServer);
                return false;
            }
        );
        try (RpcServerMessenger proxy2 = new RpcServerMessenger(
            RpcServerConfig.builder("velocity").port(freePort()).build(),
            LoggerFactory.getLogger("test-proxy2"),
            hooks
        )) {
            RpcClientMessenger lobby2 = new RpcClientMessenger(
                new RpcClientConfig("127.0.0.1", proxy2.getConfig().port(), "lobby2"),
                LoggerFactory.getLogger("test-lobby2"));
            try {
                long deadline = System.currentTimeMillis() + 10_000;
                while (!lobby2.isIdentified() && System.currentTimeMillis() < deadline) {
                    Thread.sleep(50);
                }
                assertTrue(lobby2.isIdentified(), "lobby2 should identify");
                lobby2.sendToAll("chat", "broadcast");
                Thread.sleep(500);
                assertTrue(externalForwards.contains("chat|minigames"), "known-but-unconnected backend should get an external forward, got " + externalForwards);
            } finally {
                closeQuietly(lobby2);
            }
        }
    }

    private static MessageListener capture(List<String> target, String channel) {
        return (ch, data, source) -> target.add(ch + "|" + data + "|" + source);
    }

    private static boolean contains(List<String> messages, String needle) {
        return messages.stream().anyMatch(m -> m.equals(needle));
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
