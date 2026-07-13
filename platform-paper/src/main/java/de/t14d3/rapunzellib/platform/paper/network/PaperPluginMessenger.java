package de.t14d3.rapunzellib.platform.paper.network;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.MessageBuffer;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkConstants;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class PaperPluginMessenger implements Messenger, PluginMessageListener, AutoCloseable {
    private static final long NO_CARRIER_LOG_COOLDOWN_MS = 10_000L;

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Gson gson = JsonCodecs.gson();
    private volatile String networkServerName;
    private final AtomicLong lastNoCarrierLog = new AtomicLong(0L);
    private final MessageBuffer buffer = new MessageBuffer();
    private final CarrierFlushListener flushListener;

    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();

    public PaperPluginMessenger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getSLF4JLogger();

        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, NetworkConstants.TRANSPORT_CHANNEL, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, NetworkConstants.TRANSPORT_CHANNEL);
        this.flushListener = new CarrierFlushListener();
        // Defer event registration - the owning plugin may not be fully enabled yet
        // during consumer bootstrap (onLoad). Registration is retried once on the
        // first player join if it hasn't succeeded by then.
        try {
            plugin.getServer().getPluginManager().registerEvents(flushListener, plugin);
        } catch (IllegalPluginAccessException ignored) {
            // Plugin not yet enabled; flush listener will register on first join
        }
    }

    @Override
    public void sendToAll(@NotNull String channel, @NotNull String data) {
        doSend(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.ALL, null, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data) {
        doSend(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.SERVER, serverName, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToProxy(@NotNull String channel, @NotNull String data) {
        doSend(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.PROXY, null, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
        listeners.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
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
    public @NotNull String getServerName() {
        String current = networkServerName;
        if (current != null && !current.isBlank()) {
            return current;
        }
        return "unknown";
    }

    @Override
    public @NotNull String getProxyServerName() {
        return "velocity";
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!NetworkConstants.TRANSPORT_CHANNEL.equals(channel)) return;

        String json = new String(message, StandardCharsets.UTF_8);
        NetworkEnvelope env;
        try {
            env = gson.fromJson(json, NetworkEnvelope.class);
        } catch (Exception e) {
            logger.warn("Failed to parse network envelope", e);
            return;
        }

        if (env == null || env.getChannel() == null) return;
        List<MessageListener> list = listeners.get(env.getChannel());
        if (list == null || list.isEmpty()) return;

        for (MessageListener listener : List.copyOf(list)) {
            try {
                listener.onMessage(env.getChannel(), env.getData(), env.getSourceServer());
            } catch (Exception e) {
                logger.warn("Network listener error on channel {}", env.getChannel(), e);
            }
        }
    }

    private void doSend(NetworkEnvelope env) {
        Objects.requireNonNull(env, "env");
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            buffer.enqueue(env.getChannel(), env.getData(),
                env.getTargetServer() != null ? env.getTargetServer() : "",
                toBufferTarget(env.getTarget()));
            long now = System.currentTimeMillis();
            long last = lastNoCarrierLog.get();
            if ((now - last) >= NO_CARRIER_LOG_COOLDOWN_MS && lastNoCarrierLog.compareAndSet(last, now)) {
                logger.debug(
                    "Buffering plugin message (no player carrier): target={}, channel={}, bufferSize={}",
                    env.getTarget(), env.getChannel(), buffer.size()
                );
            }
            return;
        }

        byte[] bytes = gson.toJson(env).getBytes(StandardCharsets.UTF_8);
        carrier.sendPluginMessage(plugin, NetworkConstants.TRANSPORT_CHANNEL, bytes);
    }

    private void flushBuffer() {
        int flushed = buffer.drainTo(new FlushMessenger());
        if (flushed > 0) {
            logger.debug("Flushed {} buffered plugin messages", flushed);
        }
    }

    private static MessageBuffer.Target toBufferTarget(NetworkEnvelope.Target target) {
        if (target == null) return MessageBuffer.Target.ALL;
        return switch (target) {
            case ALL -> MessageBuffer.Target.ALL;
            case PROXY -> MessageBuffer.Target.PROXY;
            case SERVER -> MessageBuffer.Target.SERVER;
        };
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(flushListener);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, NetworkConstants.TRANSPORT_CHANNEL, this);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, NetworkConstants.TRANSPORT_CHANNEL);
    }

    private final class CarrierFlushListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            if (buffer.isEmpty()) return;
            plugin.getServer().getScheduler().runTask(plugin, PaperPluginMessenger.this::flushBuffer);
        }
    }

    private final class FlushMessenger implements Messenger {
        @Override public void sendToAll(String channel, String data) {
            Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (carrier == null) throw new IllegalStateException("No carrier available");
            carrier.sendPluginMessage(plugin, NetworkConstants.TRANSPORT_CHANNEL,
                gson.toJson(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.ALL, null, getServerName(), System.currentTimeMillis())).getBytes(StandardCharsets.UTF_8));
        }
        @Override public void sendToServer(String channel, String serverName, String data) {
            Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (carrier == null) throw new IllegalStateException("No carrier available");
            carrier.sendPluginMessage(plugin, NetworkConstants.TRANSPORT_CHANNEL,
                gson.toJson(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.SERVER, serverName, getServerName(), System.currentTimeMillis())).getBytes(StandardCharsets.UTF_8));
        }
        @Override public void sendToProxy(String channel, String data) {
            Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (carrier == null) throw new IllegalStateException("No carrier available");
            carrier.sendPluginMessage(plugin, NetworkConstants.TRANSPORT_CHANNEL,
                gson.toJson(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.PROXY, null, getServerName(), System.currentTimeMillis())).getBytes(StandardCharsets.UTF_8));
        }
        @Override public void registerListener(String channel, MessageListener listener) {}
        @Override public void unregisterListener(String channel, MessageListener listener) {}
        @Override public boolean isConnected() { return !Bukkit.getOnlinePlayers().isEmpty(); }
        @Override public String getServerName() { return PaperPluginMessenger.this.getServerName(); }
        @Override public String getProxyServerName() { return PaperPluginMessenger.this.getProxyServerName(); }
    }
}
