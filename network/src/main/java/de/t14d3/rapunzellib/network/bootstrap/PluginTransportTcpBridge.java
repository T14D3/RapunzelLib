package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkDefaults;
import de.t14d3.rapunzellib.network.rpcserver.RpcClientConfig;
import de.t14d3.rapunzellib.network.rpcserver.RpcClientMessenger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Companion TCP link for the plugin-messaging transport on backend servers.
 *
 * <p>Plugin messaging can only reach the proxy through a player connection that
 * traverses it. On networks where players connect directly to the backend (or
 * when no player is available as a carrier), envelopes are silently dropped.
 * This bridge gives the plugin transport a second, player-independent carrier:
 * a direct TCP connection to the proxy's companion RPC server (see
 * {@code VelocityPluginMessenger} + {@code RpcServerMessenger} on the proxy
 * side).</p>
 *
 * <p>Routing rules:</p>
 * <ul>
 *   <li>When the TCP link is connected and identified, all sends go over TCP
 *       (deterministic, no carrier required).</li>
 *   <li>When the TCP link is unavailable, sends fall back to the wrapped plugin
 *       messenger (player-carrier plugin messaging).</li>
 *   <li>Exactly one carrier is used per envelope, so no duplicates.</li>
 *   <li>Inbound envelopes from either carrier are delivered to the same
 *       listeners.</li>
 * </ul>
 *
 * <p>The TCP client is created lazily on the first send once the server name is
 * resolved, and is re-created if the name changes. Connection attempts run on a
 * daemon thread so a missing proxy bridge never blocks the server.</p>
 */
public final class PluginTransportTcpBridge implements Messenger, AutoCloseable {
    private static final String UNKNOWN_NAME = "unknown";

    private final Messenger delegate;
    private final Logger logger;
    private final String proxyHost;
    private final int proxyPort;

    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();
    private final AtomicReference<RpcClientMessenger> tcpRef = new AtomicReference<>();
    private final AtomicBoolean tcpStartAttempted = new AtomicBoolean(false);
    private volatile long tcpStartMillis;
    private volatile String tcpBoundName;
    private volatile boolean closed;

    private PluginTransportTcpBridge(Messenger delegate, Logger logger, String proxyHost, int proxyPort) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.proxyHost = Objects.requireNonNull(proxyHost, "proxyHost");
        this.proxyPort = proxyPort;
    }

    /**
     * Wraps a plugin messenger in the TCP bridge. Reads the proxy address from
     * the transport config ({@code network.rpcServer.host/port}, defaults
     * {@code 127.0.0.1:25566}).
     *
     * @param pluginEffective the plugin messenger (possibly queue-wrapped) to wrap
     * @param transportConfig the transport configuration
     * @param logger          the logger
     * @return the bridged messenger
     */
    public static @NotNull Messenger wrap(
        @NotNull Messenger pluginEffective,
        @NotNull YamlConfig transportConfig,
        @NotNull Logger logger
    ) {
        Objects.requireNonNull(pluginEffective, "pluginEffective");
        Objects.requireNonNull(transportConfig, "transportConfig");
        Objects.requireNonNull(logger, "logger");

        String host = firstNonBlank(
            transportConfig.getString("network.rpcServer.host", null),
            NetworkDefaults.DEFAULT_RPC_HOST
        );
        long port = transportConfig.getLong("network.rpcServer.port", NetworkDefaults.DEFAULT_RPC_PORT);
        int portInt = (port >= 1 && port <= 65535) ? (int) port : NetworkDefaults.DEFAULT_RPC_PORT;
        return new PluginTransportTcpBridge(pluginEffective, logger, host, portInt);
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first.trim() : fallback;
    }

    // ── Send routing ─────────────────────────────────────────────────────────

    @Override
    public void sendToAll(@NotNull String channel, @NotNull String data) {
        if (routeOverTcp()) {
            tcpRef.get().sendToAll(channel, data);
            return;
        }
        delegate.sendToAll(channel, data);
    }

    @Override
    public void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data) {
        if (routeOverTcp()) {
            tcpRef.get().sendToServer(channel, serverName, data);
            return;
        }
        delegate.sendToServer(channel, serverName, data);
    }

    @Override
    public void sendToProxy(@NotNull String channel, @NotNull String data) {
        if (routeOverTcp()) {
            tcpRef.get().sendToProxy(channel, data);
            return;
        }
        delegate.sendToProxy(channel, data);
    }

    /**
     * Ensures the TCP link is started (if a name is available) and reports
     * whether it is usable for the current send.
     */
    private boolean routeOverTcp() {
        if (closed) return false;
        ensureTcpStarted();

        // The very first send may race the background connect (the RPC client
        // constructor blocks until connected); give it a short bounded window so
        // early envelopes do not needlessly fall back to the plugin channel.
        // The wait is limited to the first moments of the connect attempt so a
        // permanently unreachable proxy never stalls sends.
        if (tcpStartAttempted.get() && tcpRef.get() == null
            && System.currentTimeMillis() - tcpStartMillis < 10_000L) {
            long deadline = Math.min(System.currentTimeMillis() + 1_500L, tcpStartMillis + 10_000L);
            while (System.currentTimeMillis() < deadline && tcpRef.get() == null) {
                try {
                    Thread.sleep(25L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        RpcClientMessenger tcp = tcpRef.get();
        if (tcp == null) return false;
        if (!tcp.isConnected() || !tcp.isIdentified()) return false;

        // If the delegate's name changed (e.g. resolved via WHO_AM_I after
        // bootstrap), the TCP link must re-identify under the new name so the
        // proxy can route targeted envelopes to this backend.
        String currentName = delegate.getServerName();
        String boundName = tcpBoundName;
        if (boundName != null && currentName != null
            && !currentName.isBlank() && !UNKNOWN_NAME.equalsIgnoreCase(currentName)
            && !currentName.equalsIgnoreCase(boundName)) {
            logger.info("[Network] Backend name changed ({} -> {}); re-establishing TCP bridge", boundName, currentName);
            restartTcp();
            return false;
        }
        return true;
    }

    private void ensureTcpStarted() {
        if (tcpRef.get() != null || tcpStartAttempted.get()) return;

        String name = delegate.getServerName();
        if (name == null || name.isBlank() || UNKNOWN_NAME.equalsIgnoreCase(name)) {
            // Not yet bound; retry on the next send once the name resolves.
            return;
        }
        if (!tcpStartAttempted.compareAndSet(false, true)) return;

        tcpBoundName = name;
        tcpStartMillis = System.currentTimeMillis();
        Thread starter = new Thread(() -> {
            try {
                RpcClientMessenger tcp = new RpcClientMessenger(
                    new RpcClientConfig(proxyHost, proxyPort, name),
                    logger
                );
                tcpRef.set(tcp);
                reattachListeners(tcp);
                logger.info("[Network] TCP bridge to proxy established (serverName={}, {}:{})",
                    name, proxyHost, proxyPort);
            } catch (Throwable t) {
                logger.warn("[Network] Failed to establish TCP bridge to {}:{}", proxyHost, proxyPort, t);
            }
        }, "RapunzelLib-TcpBridge");
        starter.setDaemon(true);
        starter.start();
    }

    private void restartTcp() {
        RpcClientMessenger old = tcpRef.getAndSet(null);
        if (old != null) {
            try {
                old.close();
            } catch (Exception e) {
                logger.debug("[Network] Error closing stale TCP bridge", e);
            }
        }
        tcpStartAttempted.set(false);
        tcpStartMillis = 0L;
        ensureTcpStarted();
    }

    // ── Listener handling ────────────────────────────────────────────────────

    @Override
    public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
        listeners.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(listener);
        delegate.registerListener(channel, listener);
        // A server that subscribes to network channels is a network participant:
        // establish the TCP link eagerly (it is lazily created on the first SEND
        // otherwise). Without this, a mostly-idle backend has no bridge
        // connection and other servers' targeted envelopes to it are dropped -
        // e.g. a config-sync follower could never reach a quiet authority.
        ensureTcpStarted();
        RpcClientMessenger tcp = tcpRef.get();
        if (tcp != null) {
            tcp.registerListener(channel, listener);
        }
    }

    @Override
    public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
        List<MessageListener> list = listeners.get(channel);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                listeners.remove(channel);
            }
        }
        delegate.unregisterListener(channel, listener);
        RpcClientMessenger tcp = tcpRef.get();
        if (tcp != null) {
            tcp.unregisterListener(channel, listener);
        }
    }

    private void reattachListeners(RpcClientMessenger tcp) {
        for (Map.Entry<String, CopyOnWriteArrayList<MessageListener>> entry : listeners.entrySet()) {
            for (MessageListener listener : entry.getValue()) {
                tcp.registerListener(entry.getKey(), listener);
            }
        }
    }

    // ── State ────────────────────────────────────────────────────────────────

    @Override
    public boolean isConnected() {
        if (delegate.isConnected()) return true;
        RpcClientMessenger tcp = tcpRef.get();
        return tcp != null && tcp.isConnected() && tcp.isIdentified();
    }

    @Override
    public @NotNull String getServerName() {
        return delegate.getServerName();
    }

    @Override
    public @NotNull String getProxyServerName() {
        return delegate.getProxyServerName();
    }

    @Override
    public void close() {
        closed = true;
        RpcClientMessenger tcp = tcpRef.getAndSet(null);
        if (tcp != null) {
            try {
                tcp.close();
            } catch (Exception e) {
                logger.debug("[Network] Error closing TCP bridge", e);
            }
        }
        if (delegate instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.debug("[Network] Error closing delegate messenger", e);
            }
        }
    }

    @Override
    public String toString() {
        RpcClientMessenger tcp = tcpRef.get();
        return "PluginTransportTcpBridge{delegate=" + delegate.getClass().getSimpleName()
            + ", tcp=" + (tcp != null && tcp.isConnected() ? "connected" : "disconnected")
            + ", proxy=" + proxyHost + ":" + proxyPort + '}';
    }
}
