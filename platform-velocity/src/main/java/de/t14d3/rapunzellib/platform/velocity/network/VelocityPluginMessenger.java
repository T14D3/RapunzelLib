package de.t14d3.rapunzellib.platform.velocity.network;

import com.google.gson.Gson;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkConstants;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class VelocityPluginMessenger implements Messenger, AutoCloseable {
    public static final ChannelIdentifier CHANNEL_ID = MinecraftChannelIdentifier.from(NetworkConstants.TRANSPORT_CHANNEL);

    private final Object plugin;
    private final ProxyServer proxy;
    private final Logger logger;
    private final Gson gson = new Gson();

    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();

    /**
     * Optional forwarder used when Velocity cannot forward a backend->backend plugin message because the
     * target backend has no online player to act as a plugin message carrier.
     *
     * <p>Typically set to a {@code DbQueuedMessenger} wrapper so undeliverable forwards get persisted and
     * retried later.</p>
     */
    private volatile Messenger undeliverableForwarder;

    public VelocityPluginMessenger(Object plugin, ProxyServer proxy, Logger logger) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.logger = logger;

        proxy.getChannelRegistrar().register(CHANNEL_ID);
        proxy.getEventManager().register(plugin, this);
    }

    public void setUndeliverableForwarder(Messenger undeliverableForwarder) {
        this.undeliverableForwarder = undeliverableForwarder;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL_ID)) return;

        Object source = event.getSource();
        if (!(source instanceof ServerConnection serverConn)) {
            return;
        }

        String originServer = serverConn.getServerInfo().getName();
        String json = new String(event.getData(), StandardCharsets.UTF_8);

        NetworkEnvelope env;
        try {
            env = gson.fromJson(json, NetworkEnvelope.class);
        } catch (Exception e) {
            logger.warn("Failed to parse network envelope from backend {}: {}", originServer, e.getMessage());
            return;
        }

        if (env == null || env.getChannel() == null) return;
        env.setSourceServer(originServer);

        deliverToLocalListeners(env);

        switch (env.getTarget()) {
            case PROXY -> {
                // handled locally only
            }
            case ALL -> forwardToAllBackends(env);
            case SERVER -> {
                if (env.getTargetServer() != null && !env.getTargetServer().isBlank()) {
                    boolean forwarded = forwardToBackend(env.getTargetServer(), env);
                    if (!forwarded) {
                        queueForwardToBackend(env.getTargetServer(), env);
                    }
                }
            }
            default -> {
                // ignore
            }
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    @Override
    public void sendToAll(String channel, String data) {
        forwardToAllBackends(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.ALL, null, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToServer(String channel, String serverName, String data) {
        forwardToBackend(serverName, new NetworkEnvelope(channel, data, NetworkEnvelope.Target.SERVER, serverName, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToProxy(String channel, String data) {
        deliverToLocalListeners(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.PROXY, null, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void registerListener(String channel, MessageListener listener) {
        listeners.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unregisterListener(String channel, MessageListener listener) {
        List<MessageListener> list = listeners.get(channel);
        if (list == null) return;
        list.remove(listener);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public String getServerName() {
        return getProxyServerName();
    }

    @Override
    public String getProxyServerName() {
        return "velocity";
    }

    private void forwardToAllBackends(NetworkEnvelope env) {
        for (String serverName : proxy.getAllServers().stream().map(s -> s.getServerInfo().getName()).toList()) {
            if (serverName.equalsIgnoreCase(env.getSourceServer())) continue;
            boolean forwarded = forwardToBackend(serverName, env);
            if (!forwarded) {
                queueForwardToBackend(serverName, env);
            }
        }
    }

    private boolean forwardToBackend(String serverName, NetworkEnvelope env) {
        Optional<com.velocitypowered.api.proxy.server.RegisteredServer> rsOpt = proxy.getServer(serverName);
        if (rsOpt.isEmpty()) return false;

        Optional<Player> carrier = proxy.getAllPlayers().stream()
            .filter(p -> p.getCurrentServer().map(sc -> sc.getServerInfo().getName().equalsIgnoreCase(serverName)).orElse(false))
            .findFirst();
        if (carrier.isEmpty()) return false;

        Optional<ServerConnection> connection = carrier.get().getCurrentServer();
        if (connection.isEmpty()) return false;

        byte[] bytes = gson.toJson(env).getBytes(StandardCharsets.UTF_8);
        connection.get().sendPluginMessage(CHANNEL_ID, bytes);
        return true;
    }

    private void queueForwardToBackend(String serverName, NetworkEnvelope env) {
        Messenger forwarder = undeliverableForwarder;
        if (forwarder == null) return;
        if (env == null || env.getChannel() == null) return;
        try {
            forwarder.sendToServer(env.getChannel(), serverName, env.getData());
        } catch (Exception e) {
            logger.debug("Failed to queue backend forward for {} on channel {}: {}", serverName, env.getChannel(), e.getMessage());
        }
    }

    private void deliverToLocalListeners(NetworkEnvelope env) {
        List<MessageListener> list = listeners.get(env.getChannel());
        if (list == null || list.isEmpty()) return;
        for (MessageListener listener : List.copyOf(list)) {
            try {
                listener.onMessage(env.getChannel(), env.getData(), env.getSourceServer());
            } catch (Exception e) {
                logger.warn("Network listener error on channel {}: {}", env.getChannel(), e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        // Velocity does not currently expose a stable unregister API for this pattern across versions.
    }
}

