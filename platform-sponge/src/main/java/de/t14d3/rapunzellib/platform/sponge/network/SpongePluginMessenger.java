package de.t14d3.rapunzellib.platform.sponge.network;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkConstants;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.network.EngineConnectionState;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.ChannelManager;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataChannel;
import org.spongepowered.plugin.PluginContainer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sponge transport using the raw play channel forwarded by a proxy (e.g. Velocity).
 *
 * <p>Requires a player connection to carry messages, same as Paper/Fabric plugin
 * messaging. Messages are wrapped in a {@link NetworkEnvelope} and serialized as
 * UTF-8 JSON over the {@link NetworkConstants#TRANSPORT_CHANNEL}.</p>
 */
public final class SpongePluginMessenger implements Messenger, AutoCloseable {
    private static final long NO_CARRIER_LOG_COOLDOWN_MS = 10_000L;

    private final PluginContainer plugin;
    private final Logger logger;
    private final Gson gson = JsonCodecs.gson();
    private final RawPlayDataChannel channel;
    private final AtomicLong lastNoCarrierLog = new AtomicLong(0L);

    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();
    private volatile String networkServerName;

    public SpongePluginMessenger(@NotNull PluginContainer plugin, @NotNull Logger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");

        ChannelManager channelManager = Sponge.game().channelManager();
        RawDataChannel rawChannel = channelManager.ofType(
            ResourceKey.resolve(NetworkConstants.TRANSPORT_CHANNEL),
            RawDataChannel.class
        );
        this.channel = rawChannel.play();
        this.channel.addHandler(this::handlePayload);
    }

    @Override
    public void sendToAll(@NotNull String channelName, @NotNull String data) {
        sendEnvelope(new NetworkEnvelope(channelName, data, NetworkEnvelope.Target.ALL, null, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToServer(@NotNull String channelName, @NotNull String serverName, @NotNull String data) {
        sendEnvelope(new NetworkEnvelope(channelName, data, NetworkEnvelope.Target.SERVER, serverName, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToProxy(@NotNull String channelName, @NotNull String data) {
        sendEnvelope(new NetworkEnvelope(channelName, data, NetworkEnvelope.Target.PROXY, null, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void registerListener(@NotNull String channelName, @NotNull MessageListener listener) {
        listeners.computeIfAbsent(channelName, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unregisterListener(@NotNull String channelName, @NotNull MessageListener listener) {
        List<MessageListener> list = listeners.get(channelName);
        if (list == null) return;
        list.remove(listener);
    }

    @Override
    public boolean isConnected() {
        if (!Sponge.isServerAvailable()) return false;
        return !Sponge.server().onlinePlayers().isEmpty();
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

    private void handlePayload(ChannelBuf buf, EngineConnectionState connection) {
        String json;
        try {
            json = buf.readUTF();
        } catch (Exception e) {
            logger.warn("Failed to read network payload", e);
            return;
        }

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

    private void sendEnvelope(NetworkEnvelope env) {
        if (!Sponge.isServerAvailable()) {
            logNoCarrier(env);
            return;
        }
        ServerPlayer carrier = Sponge.server().onlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            logNoCarrier(env);
            return;
        }

        String json = gson.toJson(env);
        CompletableFuture<Void> future = channel.sendTo(carrier, buf -> buf.writeUTF(json));
        future.whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                logger.debug("Failed to send plugin message on channel {}", env.getChannel(), throwable);
            }
        });
    }

    private void logNoCarrier(NetworkEnvelope env) {
        long now = System.currentTimeMillis();
        long last = lastNoCarrierLog.get();
        if ((now - last) >= NO_CARRIER_LOG_COOLDOWN_MS && lastNoCarrierLog.compareAndSet(last, now)) {
            logger.debug(
                "Dropping plugin message (no player carrier available): target={}, channel={}",
                env.getTarget(), env.getChannel()
            );
        }
    }

    @Override
    public void close() {
        channel.removeHandler(this::handlePayload);
    }
}
