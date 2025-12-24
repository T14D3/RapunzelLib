package de.t14d3.rapunzellib.platform.paper.network;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkConstants;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PaperPluginMessenger implements Messenger, PluginMessageListener, AutoCloseable {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Gson gson = new Gson();
    private volatile String networkServerName;

    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();

    public PaperPluginMessenger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getSLF4JLogger();

        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, NetworkConstants.TRANSPORT_CHANNEL, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, NetworkConstants.TRANSPORT_CHANNEL);
    }

    @Override
    public void sendToAll(String channel, String data) {
        sendEnvelope(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.ALL, null, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToServer(String channel, String serverName, String data) {
        sendEnvelope(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.SERVER, serverName, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToProxy(String channel, String data) {
        sendEnvelope(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.PROXY, null, getServerName(), System.currentTimeMillis()));
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
        return !Bukkit.getOnlinePlayers().isEmpty();
    }

    public boolean hasNetworkServerName() {
        String current = networkServerName;
        return current != null && !current.isBlank();
    }

    public void setNetworkServerName(String networkServerName) {
        if (networkServerName == null || networkServerName.isBlank()) return;
        this.networkServerName = networkServerName;
    }

    @Override
    public String getServerName() {
        String current = networkServerName;
        if (current != null && !current.isBlank()) {
            return current;
        }
        return "unknown";
    }

    @Override
    public String getProxyServerName() {
        return "velocity";
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!NetworkConstants.TRANSPORT_CHANNEL.equals(channel)) return;

        String json = new String(message, StandardCharsets.UTF_8);
        NetworkEnvelope env;
        try {
            env = gson.fromJson(json, NetworkEnvelope.class);
        } catch (Exception e) {
            logger.warn("Failed to parse network envelope: {}", e.getMessage());
            return;
        }

        if (env == null || env.getChannel() == null) return;
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

    private void sendEnvelope(NetworkEnvelope env) {
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) return;

        byte[] bytes = gson.toJson(env).getBytes(StandardCharsets.UTF_8);
        carrier.sendPluginMessage(plugin, NetworkConstants.TRANSPORT_CHANNEL, bytes);
    }

    @Override
    public void close() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, NetworkConstants.TRANSPORT_CHANNEL, this);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, NetworkConstants.TRANSPORT_CHANNEL);
    }
}
