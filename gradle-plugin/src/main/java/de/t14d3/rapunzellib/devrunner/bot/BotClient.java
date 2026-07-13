package de.t14d3.rapunzellib.devrunner.bot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.session.ClientNetworkSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHeldSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundAttackPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Wraps an MCProtocolLib connection to control a Minecraft bot client.
 * Connects in offline mode (no Mojang authentication required).
 */
public class BotClient {

    private final String name;
    private final String host;
    private final int port;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final List<String> chatMessages = new CopyOnWriteArrayList<>();
    private final List<CompletableFuture<String>> chatFutures = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> chatCallbacks = new CopyOnWriteArrayList<>();

    private ClientNetworkSession session;
    private String currentServer;

    private double x, y, z;
    private float yaw, pitch;
    private float health;
    private int food;
    private float saturation;
    private int heldItemSlot;
    private String heldItemMaterial = "unknown";
    private String gameMode = "unknown";
    private int actionSequence;
    private volatile int openContainerId = -1;
    private final java.util.Map<Integer, String> entityTypes = new java.util.concurrent.ConcurrentHashMap<>();
    private int botEntityId = -1;

    public BotClient(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.currentServer = host + ":" + port;
    }

    public void connect() throws Exception {
        if (connected.get()) {
            throw new IllegalStateException("Bot '" + name + "' is already connected");
        }
        MinecraftProtocol protocol = new MinecraftProtocol(name);
        session = ClientNetworkSessionFactory.factory()
            .setAddress(host, port)
            .setProtocol(protocol)
            .setPacketHandlerExecutor(Executors.newSingleThreadExecutor())
            .create();

        CompletableFuture<Void> loginFuture = new CompletableFuture<>();
        session.addListener(new SessionAdapter() {
            @Override
            public void packetReceived(Session s, Packet packet) {
if (packet instanceof ClientboundLoginPacket loginPacket) {
                    connected.set(true);
                    loginFuture.complete(null);
                    botEntityId = loginPacket.getEntityId();
                    if (loginPacket.getCommonPlayerSpawnInfo() != null
                        && loginPacket.getCommonPlayerSpawnInfo().getGameMode() != null) {
                        gameMode = loginPacket.getCommonPlayerSpawnInfo().getGameMode().name().toLowerCase();
                    }
                    botEntityId = loginPacket.getEntityId();
                }
            }
            @Override
            public void disconnected(DisconnectedEvent event) {
                connected.set(false);
                if (!loginFuture.isDone()) {
                    String reason = event.getReason() != null
                        ? event.getReason().toString()
                        : (event.getCause() != null ? event.getCause().getMessage() : "Unknown reason");
                    loginFuture.completeExceptionally(new RuntimeException("Bot '" + name + "' disconnected: " + reason));
                }
            }
        });
        session.addListener(new BotSessionListener());
        try {
            session.connect(false);
        } catch (Exception e) {
            throw new RuntimeException("Bot '" + name + "' connection failed: " + e.getMessage(), e);
        }
        System.out.println("[devrunner] Bot '" + name + "' waiting for join game on " + host + ":" + port + "...");
        try {
            loginFuture.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            session.disconnect("Login timeout");
            connected.set(false);
            throw new RuntimeException("Bot '" + name + "' login timed out after 60s");
        }
        System.out.println("[devrunner] Bot '" + name + "' join game received, fully logged in");
    }

    public void sendChat(String message) {
        if (!connected.get() || session == null) {
            throw new IllegalStateException("Bot '" + name + "' is not connected");
        }
        if (message.startsWith("/")) {
            String command = message.substring(1);
            session.send(new ServerboundChatCommandPacket(command));
        } else {
            session.send(new ServerboundChatPacket(message, System.currentTimeMillis(), 0L, new byte[0], 0, new BitSet(), 0));
        }
    }

    public void digBlock(int x, int y, int z, int direction) {
        if (!connected.get() || session == null) return;
        Vector3i pos = Vector3i.from(x, y, z);
        Direction dir = Direction.values()[direction];
        int seq = actionSequence++;
        session.send(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, pos, dir, seq));
        session.send(new ServerboundPlayerActionPacket(PlayerAction.FINISH_DIGGING, pos, dir, seq));
    }

    public void useItemOn(int x, int y, int z, int hand, int direction) {
        if (!connected.get() || session == null) return;
        Vector3i pos = Vector3i.from(x, y, z);
        Direction dir = Direction.values()[direction];
        Hand handEnum = hand == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        int seq = actionSequence++;
        session.send(new ServerboundUseItemOnPacket(pos, dir, handEnum, 0.5f, 0.5f, 0.5f, false, false, seq));
    }

    public void attackEntity(int entityId) {
        if (!connected.get() || session == null) return;
        session.send(new ServerboundAttackPacket(entityId));
        session.send(new ServerboundSwingPacket(Hand.MAIN_HAND));
    }

    public void interactEntity(int entityId, int hand) {
        if (!connected.get() || session == null) return;
        Hand handEnum = hand == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        session.send(new ServerboundInteractPacket(entityId, handEnum, null, false));
        session.send(new ServerboundSwingPacket(handEnum));
    }

    public void swingHand(int hand) {
        if (!connected.get() || session == null) return;
        Hand handEnum = hand == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        session.send(new ServerboundSwingPacket(handEnum));
    }

    public void setHeldItemSlot(int slot) {
        if (!connected.get() || session == null) return;
        session.send(new ServerboundSetCarriedItemPacket(slot));
    }

    public void disconnect() {
        connected.set(false);
        if (session != null) {
            session.disconnect("Bot shutdown");
            session = null;
        }
        for (CompletableFuture<String> future : chatFutures) {
            future.complete("");
        }
        chatFutures.clear();
    }

    public boolean isConnected() { return connected.get() && session != null; }
    public String getCurrentServer() { return currentServer; }
    public String getName() { return name; }

    public List<String> getChatMessages() {
        List<String> messages = new ArrayList<>(chatMessages);
        chatMessages.clear();
        return messages;
    }

    public void clearChatMessages() { chatMessages.clear(); }

    public void addChatCallback(Consumer<String> callback) { chatCallbacks.add(callback); }

    public String waitForChat(String text, long timeoutMs) throws Exception {
        for (String msg : chatMessages) {
            if (msg != null && msg.toLowerCase().contains(text.toLowerCase())) return msg;
        }
        CompletableFuture<String> future = new CompletableFuture<>();
        chatFutures.add(future);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Bot '" + name + "' did not receive chat containing \"" + text + "\" within " + timeoutMs + "ms");
        } finally {
            chatFutures.remove(future);
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getHealth() { return health; }
    public int getFood() { return food; }
    public float getSaturation() { return saturation; }
    public int getHeldItemSlot() { return heldItemSlot; }
    public String getHeldItemMaterial() { return heldItemMaterial; }
    public String getGameMode() { return gameMode; }

    public int getOpenContainerId() { return openContainerId; }

    public int getBotEntityId() { return botEntityId; }

    /**
     * Returns the entity IDs of all tracked entities matching the given type name.
     */
    public java.util.List<Integer> findEntities(String typeName) {
        String upper = typeName.toUpperCase();
        java.util.List<Integer> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Integer, String> entry : entityTypes.entrySet()) {
            String entryUpper = entry.getValue() != null ? entry.getValue().toUpperCase() : "";
            if (entryUpper.equals(upper) || entryUpper.replace("minecraft:", "").equals(upper)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public void moveTo(double targetX, double targetY, double targetZ) {
        if (!connected.get() || session == null) return;
        session.send(new ServerboundMovePlayerPosRotPacket(true, false, targetX, targetY, targetZ, yaw, pitch));
    }

    private class BotSessionListener extends SessionAdapter {
        @Override
        public void packetReceived(Session session, Packet packet) {
            if (packet instanceof ClientboundPlayerPositionPacket posPacket) {
                x = posPacket.getPosition().getX();
                y = posPacket.getPosition().getY();
                z = posPacket.getPosition().getZ();
                yaw = posPacket.getYRot();
                pitch = posPacket.getXRot();
                session.send(new ServerboundAcceptTeleportationPacket(posPacket.getId()));
                return;
            }
            if (packet instanceof ClientboundSetHealthPacket healthPacket) {
                health = healthPacket.getHealth();
                food = healthPacket.getFood();
                saturation = healthPacket.getSaturation();
                return;
            }
            if (packet instanceof ClientboundOpenScreenPacket openScreen) {
                openContainerId = openScreen.getContainerId();
                return;
            }
            if (packet instanceof ClientboundContainerClosePacket closeContainer) {
                openContainerId = -1;
                return;
            }
            if (packet instanceof ClientboundAddEntityPacket addEntity) {
                int id = addEntity.getEntityId();
                String typeName = addEntity.getType() != null ? addEntity.getType().name() : "unknown";
                entityTypes.put(id, typeName);
                return;
            }
            if (packet instanceof ClientboundRemoveEntitiesPacket removeEntities) {
                for (int id : removeEntities.getEntityIds()) {
                    entityTypes.remove(id);
                }
                return;
            }
            if (packet instanceof ClientboundSetHeldSlotPacket heldSlotPacket) {
                heldItemSlot = heldSlotPacket.getSlot();
                heldItemMaterial = "unknown";
                return;
            }
            if (packet instanceof ClientboundGameEventPacket gameEvent) {
                if (gameEvent.getNotification() == GameEvent.CHANGE_GAME_MODE) {
                    if (gameEvent.getValue() instanceof GameMode mode) {
                        gameMode = mode.name().toLowerCase();
                    }
                }
                return;
            }
            String message = extractChatMessage(packet);
            if (message != null) {
                chatMessages.add(message);
                for (Consumer<String> callback : chatCallbacks) {
                    try { callback.accept(message); } catch (Exception ignored) {}
                }
                for (CompletableFuture<String> future : chatFutures) {
                    if (!future.isDone()) future.complete(message);
                }
            }
        }

        @Override
        public void disconnected(DisconnectedEvent event) {
            connected.set(false);
            String reason = event.getReason() != null ? event.getReason().toString()
                : (event.getCause() != null ? event.getCause().getMessage() : "Unknown");
            for (CompletableFuture<String> future : chatFutures) {
                future.completeExceptionally(new RuntimeException("Bot '" + name + "' disconnected: " + reason));
            }
            chatFutures.clear();
        }

        private String extractChatMessage(Packet packet) {
            if (packet instanceof ClientboundPlayerChatPacket playerChat) {
                String content = playerChat.getContent();
                if (content != null && !content.isEmpty()) return content;
                if (playerChat.getUnsignedContent() != null) return serializeComponent(playerChat.getUnsignedContent());
                return null;
            }
            if (packet instanceof ClientboundSystemChatPacket systemChat) {
                if (systemChat.getContent() != null) return serializeComponent(systemChat.getContent());
                return null;
            }
            if (packet instanceof ClientboundDisguisedChatPacket disguisedChat) {
                if (disguisedChat.getMessage() != null) return serializeComponent(disguisedChat.getMessage());
                return null;
            }
            if (packet instanceof ClientboundSetActionBarTextPacket actionBar) {
                if (actionBar.getText() != null) return serializeComponent(actionBar.getText());
                return null;
            }
            return null;
        }

        private String serializeComponent(Component component) {
            if (component == null) return null;
            try { return PlainTextComponentSerializer.plainText().serialize(component); }
            catch (Exception e) { return component.toString(); }
        }
    }
}