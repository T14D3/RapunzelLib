package de.t14d3.rapunzellib.platform.fabric.network;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkConstants;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fabric transport using the plugin messaging channel forwarded by a proxy (e.g. Velocity).
 *
 * <p>Requires a player connection to carry messages, same as Paper plugin messaging.</p>
 */
public final class FabricPluginMessenger implements Messenger, AutoCloseable {
    private final MinecraftServer server;
    private final Logger logger;
    private final Gson gson = new Gson();
    private final ResourceLocation channelId = ResourceLocation.parse(NetworkConstants.TRANSPORT_CHANNEL);

    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();
    private volatile String networkServerName;

    public FabricPluginMessenger(MinecraftServer server, Logger logger) {
        this.server = Objects.requireNonNull(server, "server");
        this.logger = Objects.requireNonNull(logger, "logger");

        PayloadTypeRegistry.playC2S().register(BridgePayload.TYPE, BridgePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BridgePayload.TYPE, BridgePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BridgePayload.TYPE, this::handlePluginMessage);
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
        return !server.getPlayerList().getPlayers().isEmpty();
    }

    @Override
    public String getServerName() {
        String current = networkServerName;
        if (current != null && !current.isBlank()) return current;
        return "unknown";
    }

    @Override
    public String getProxyServerName() {
        return "velocity";
    }

    public void setNetworkServerName(String networkServerName) {
        if (networkServerName == null || networkServerName.isBlank()) return;
        this.networkServerName = networkServerName;
    }

    private void handlePluginMessage(BridgePayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            NetworkEnvelope env;
            try {
                env = gson.fromJson(payload.json(), NetworkEnvelope.class);
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
        });
    }

    private void sendEnvelope(NetworkEnvelope env) {
        ServerPlayer carrier = server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
        if (carrier == null) return;

        ServerPlayNetworking.send(carrier, new BridgePayload(gson.toJson(env)));
    }

    @Override
    public void close() {
        ServerPlayNetworking.unregisterGlobalReceiver(channelId);
    }

    /**
     * Payload carrying the JSON-encoded NetworkEnvelope over the proxy bridge.
     */
    public record BridgePayload(String json) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<BridgePayload> TYPE = new Type<>(ResourceLocation.parse(NetworkConstants.TRANSPORT_CHANNEL));
        public static final StreamCodec<FriendlyByteBuf, BridgePayload> CODEC = StreamCodec.of(
                BridgePayload::write,
                BridgePayload::read
        );

        @Override
        public @NotNull Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
            return TYPE;
        }

        private static BridgePayload read(FriendlyByteBuf buf) {
            return new BridgePayload(buf.readUtf());
        }

        private static void write(FriendlyByteBuf buf, BridgePayload payload) {
            buf.writeUtf(payload.json());
        }
    }
}
