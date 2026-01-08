package de.t14d3.rapunzellib.platform.neoforge.network;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkConstants;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NeoForge transport using the vanilla custom-payload channel forwarded by a proxy (e.g. Velocity).
 *
 * <p>Requires a player connection to carry messages, same as Paper/Fabric plugin messaging.</p>
 */
public final class NeoForgePluginMessenger implements Messenger, AutoCloseable {
    private static final List<NeoForgePluginMessenger> MESSENGERS = new CopyOnWriteArrayList<>();

    private final MinecraftServer server;
    private final Logger logger;
    private final Gson gson = new Gson();

    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();
    private volatile String networkServerName;

    public NeoForgePluginMessenger(MinecraftServer server, Logger logger) {     
        this.server = Objects.requireNonNull(server, "server");
        this.logger = Objects.requireNonNull(logger, "logger");
        MESSENGERS.add(this);
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
        var playerList = server.getPlayerList();
        return !playerList.getPlayers().isEmpty();
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

    void handlePluginMessage(String json) {
        if (json == null || json.isEmpty()) return;

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
        if (!server.isSameThread()) {
            server.execute(() -> sendEnvelope(env));
            return;
        }

        var playerList = server.getPlayerList();

        ServerPlayer carrier = playerList.getPlayers().stream().findFirst().orElse(null);
        if (carrier == null) return;

        String json = gson.toJson(env);
        // Avoid sending huge payloads; the vanilla limit is tight.
        if (json.getBytes(StandardCharsets.UTF_8).length > 30_000) return;

        PacketDistributor.sendToPlayer(carrier, new BridgePayload(json));
    }

    @Override
    public void close() {
        MESSENGERS.remove(this);
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("rapunzellib_platform_neoforge");
        registrar.playToClient(BridgePayload.TYPE, BridgePayload.CODEC);
        registrar.playToServer(BridgePayload.TYPE, BridgePayload.CODEC, NeoForgePluginMessenger::handleServerbound);
    }

    private static void handleServerbound(BridgePayload payload, IPayloadContext context) {
        if (payload == null || payload.json() == null) return;
        if (context == null) return;
        if (context.flow() != PacketFlow.SERVERBOUND) return;
        if (!(context.player() instanceof ServerPlayer)) return;

        context.enqueueWork(() -> {
            for (NeoForgePluginMessenger messenger : List.copyOf(MESSENGERS)) {
                try {
                    messenger.handlePluginMessage(payload.json());
                } catch (Exception ignored) {
                }
            }
        });
    }

    /**
     * Payload carrying the JSON-encoded NetworkEnvelope over the proxy bridge.
     */
    public record BridgePayload(String json) implements CustomPacketPayload {
        public static final Type<BridgePayload> TYPE = new Type<>(ResourceLocation.parse(NetworkConstants.TRANSPORT_CHANNEL));
        public static final StreamCodec<RegistryFriendlyByteBuf, BridgePayload> CODEC = StreamCodec.of(
            BridgePayload::write,
            BridgePayload::read
        );

        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static BridgePayload read(RegistryFriendlyByteBuf buf) {
            return new BridgePayload(buf.readUtf());
        }

        private static void write(RegistryFriendlyByteBuf buf, BridgePayload payload) {
            buf.writeUtf(payload.json());
        }
    }
}
