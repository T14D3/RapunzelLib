package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.SnakeYamlConfigService;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.rpcserver.RpcServerConfig;
import de.t14d3.rapunzellib.network.rpcserver.RpcServerMessenger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the backend-side {@link PluginTransportTcpBridge} wrapper: it routes
 * sends over the TCP link when available (single carrier), delivers inbound TCP
 * envelopes to the same listeners as the wrapped messenger, and keeps the
 * plugin messenger as the fallback when TCP is unavailable.
 */
final class PluginTransportTcpBridgeWrapperTest {

    @TempDir
    Path tempDir;

    private int proxyPort;
    private RpcServerMessenger proxy;
    private final List<String> proxyMessages = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        proxyPort = freePort();
        proxy = new RpcServerMessenger(
            RpcServerConfig.builder("velocity").port(proxyPort).build(),
            LoggerFactory.getLogger("test-proxy")
        );
        proxy.registerListener("chat", (channel, data, source) -> proxyMessages.add(channel + "|" + data + "|" + source));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (proxy != null) {
            proxy.close();
        }
    }

    @Test
    void routesSendsOverTcpWhenAvailableAndDeliversInboundToSameListeners() throws Exception {
        AtomicInteger delegateSends = new AtomicInteger();
        CountingPluginMessenger delegate = new CountingPluginMessenger("lobby", delegateSends);
        Messenger bridge = PluginTransportTcpBridge.wrap(delegate, config(proxyPort), LoggerFactory.getLogger("test-bridge"));

        List<String> bridgeMessages = new CopyOnWriteArrayList<>();
        bridge.registerListener("chat", (channel, data, source) -> bridgeMessages.add(channel + "|" + data + "|" + source));

        bridge.sendToAll("chat", "hello");
        bridge.sendToProxy("chat", "to-proxy");

        // Give the lazy TCP link time to connect and deliver.
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline && proxyMessages.isEmpty()) {
            Thread.sleep(50);
        }

        assertFalse(proxyMessages.isEmpty(), "proxy should receive envelopes over the TCP link, got " + proxyMessages);
        assertTrue(proxyMessages.contains("chat|hello|lobby"), "broadcast should reach the proxy, got " + proxyMessages);
        assertTrue(proxyMessages.contains("chat|to-proxy|lobby"), "proxy-addressed envelope should reach the proxy, got " + proxyMessages);
        assertEquals(0, delegateSends.get(), "TCP link should be the single carrier when connected");
        // sendToAll loops back to the sender's own listeners (same semantics as the
        // plugin-channel broadcast that the proxy echoes back to the source).
        assertTrue(bridgeMessages.contains("chat|hello|lobby"),
            "sendToAll should loop back to the sender's listeners, got " + bridgeMessages);
        assertFalse(bridgeMessages.contains("chat|to-proxy|lobby"),
            "sendToProxy must not loop back to the sender, got " + bridgeMessages);

        // Inbound TCP envelopes (proxy -> backend) must reach the bridge listeners.
        proxy.sendToServer("chat", "lobby", "from-proxy");
        deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline
            && !bridgeMessages.contains("chat|from-proxy|velocity")) {
            Thread.sleep(50);
        }
        assertTrue(bridgeMessages.contains("chat|from-proxy|velocity"),
            "inbound TCP envelope should be delivered to bridge listeners, got " + bridgeMessages);
    }

    @Test
    void fallsBackToPluginMessengerWhenTcpUnavailable() throws Exception {
        AtomicInteger delegateSends = new AtomicInteger();
        CountingPluginMessenger delegate = new CountingPluginMessenger("lobby", delegateSends);
        // Point the bridge at a port nothing listens on.
        int deadPort = freePort();
        Messenger bridge = PluginTransportTcpBridge.wrap(delegate, config(deadPort), LoggerFactory.getLogger("test-bridge"));
        AtomicBoolean delegateReceived = new AtomicBoolean(false);
        delegate.registerListener("chat", (channel, data, source) -> delegateReceived.set(true));

        bridge.sendToAll("chat", "fallback");

        // The TCP link cannot connect; the send must fall back to the delegate.
        long deadline = System.currentTimeMillis() + 4_000;
        while (System.currentTimeMillis() < deadline && delegateSends.get() == 0) {
            Thread.sleep(50);
        }
        assertTrue(delegateSends.get() > 0, "plugin messenger should receive the send when TCP is unavailable");
        // The delegate delivers locally (in-memory loopback).
        deadline = System.currentTimeMillis() + 4_000;
        while (System.currentTimeMillis() < deadline && !delegateReceived.get()) {
            Thread.sleep(50);
        }
        assertTrue(delegateReceived.get(), "delegate should deliver the payload to its listeners");
    }

    private YamlConfig config(int port) throws IOException {
        Files.writeString(tempDir.resolve("config-" + port + ".yml"),
            "network:\n  rpcServer:\n    host: 127.0.0.1\n    port: " + port + "\n");
        ConfigService service = new SnakeYamlConfigService(path -> java.util.Optional.empty(), LoggerFactory.getLogger("test-config"));
        return service.load(tempDir.resolve("config-" + port + ".yml"));
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /** Minimal messenger that counts sends and delivers locally, mimicking a plugin messenger. */
    private static final class CountingPluginMessenger implements Messenger {
        private final String name;
        private final AtomicInteger sends;
        private final List<ListenerEntry> listeners = new CopyOnWriteArrayList<>();

        CountingPluginMessenger(String name, AtomicInteger sends) {
            this.name = name;
            this.sends = sends;
        }

        @Override
        public void sendToAll(String channel, String data) {
            sends.incrementAndGet();
            deliver(channel, data);
        }

        @Override
        public void sendToServer(String channel, String serverName, String data) {
            sends.incrementAndGet();
            if (serverName != null && serverName.equalsIgnoreCase(name)) {
                deliver(channel, data);
            }
        }

        @Override
        public void sendToProxy(String channel, String data) {
            sends.incrementAndGet();
        }

        @Override
        public void registerListener(String channel, MessageListener listener) {
            listeners.add(new ListenerEntry(channel, listener));
        }

        @Override
        public void unregisterListener(String channel, MessageListener listener) {
            listeners.removeIf(e -> e.channel.equals(channel) && e.listener == listener);
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public @org.jetbrains.annotations.NotNull String getServerName() {
            return name;
        }

        @Override
        public @org.jetbrains.annotations.NotNull String getProxyServerName() {
            return "velocity";
        }

        private void deliver(String channel, String data) {
            for (ListenerEntry entry : listeners) {
                if (entry.channel.equals(channel)) {
                    entry.listener.onMessage(channel, data, name);
                }
            }
        }

        private record ListenerEntry(String channel, MessageListener listener) {
        }
    }
}
