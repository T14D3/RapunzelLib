package de.t14d3.rapunzellib.devrunner.bot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.DropItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ShiftClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.MoveToHotbarAction;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandSuggestionsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityPositionSyncPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHeldSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundSetCursorItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundSetPlayerInventoryPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundBlockChangedAckPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundExplodePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundAttackPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    // Whether a server-reported position has been received yet. Movement
    // confirmations must not be sent before the first ClientboundPlayerPositionPacket:
    // with the initial cache of (0,0,0), a confirm packet would be a bogus
    // teleport and Paper's anti-cheat would fail-move the bot (setting
    // awaitingPositionFromClient, which silently drops subsequent interact
    // packets).
    private volatile boolean hasPosition = false;
    // Whether the bot is currently dead (health <= 0). Dead players are
    // "immobile" on the server, which silently drops every use/attack packet,
    // so the bot auto-respawns as soon as death is detected.
    private volatile boolean dead = false;
    private final AtomicInteger respawnAttempts = new AtomicInteger(0);
    // Paper 26.x starts a ~3s "client loaded" timeout when a player joins the
    // server AND restarts it after every respawn; while it runs, the server
    // silently drops every use-item/use-item-on/interact packet
    // (hasClientLoaded() == false). Physical actions are held back until the
    // window has passed. Both timestamps are Long.MIN_VALUE until the
    // corresponding server event has been observed.
    private static final long CLIENT_READY_DELAY_MS = 3500L;
    private volatile long lastJoinNanos = Long.MIN_VALUE;
    private volatile long lastRespawnNanos = Long.MIN_VALUE;
    // The bot's current sneaking state (last sendPlayerInput value). The
    // serverbound interact packet carries its own sneak flag which overwrites
    // the player's shift state server-side, so it must mirror the bot's state.
    private volatile boolean sneaking = false;
    private float health;
    private int food;
    private float saturation;
    private int heldItemSlot;
    private String heldItemMaterial = "unknown";
    private String gameMode = "unknown";
    private int actionSequence;
    private final AtomicInteger interactionSequence = new AtomicInteger();
    private volatile int openContainerId = -1;
    private final java.util.Map<Integer, EntitySnapshot> entitySnapshots = new java.util.concurrent.ConcurrentHashMap<>();
    private int botEntityId = -1;

    // Latest server-reported player abilities. {@code null} until the server
    // first sends a {@code ClientboundPlayerAbilitiesPacket}.
    private volatile AbilitiesSnapshot abilities = null;

    // Outstanding tab-completion requests, keyed by client-chosen transaction id.
    private final java.util.concurrent.ConcurrentHashMap<Integer,
            java.util.concurrent.CompletableFuture<java.util.List<Suggestion>>> pendingSuggestions =
            new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger suggestionTxnCounter = new java.util.concurrent.atomic.AtomicInteger(0);

    // Entity listeners fire whenever an entity snapshot is created/updated/removed.
    private final java.util.List<Runnable> entityListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    // Abilities listeners fire on every {@code ClientboundPlayerAbilitiesPacket}.
    private final java.util.List<java.util.function.Consumer<AbilitiesSnapshot>> abilitiesListeners =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    // Block-change tracking - accumulates recent block state changes from
    // the server (ClientboundBlockUpdatePacket / ClientboundSectionBlocksUpdatePacket).
    private final java.util.List<BlockSnapshot> blockSnapshots = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final int MAX_BLOCK_SNAPSHOTS = 1000;
    // Block listeners fire whenever block snapshots are added.
    private final java.util.List<Runnable> blockListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    // Explosion tracking - latest explosion event from the server.
    private volatile ExplosionSnapshot latestExplosion;
    // Explosion listeners fire whenever an explosion is received.
    private final java.util.List<Runnable> explosionListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    // Per-container inventory state. Keyed by containerId (0 = player inventory).
    // The player inventory is pre-populated with an empty 46-slot state so that
    // TestSnapshot(0) always works even before the server sends a full sync.
    private final java.util.concurrent.ConcurrentHashMap<Integer, ContainerState> containers = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile int cursorItemContainerId = 0; // which container the cursor belongs to
    // Listeners fired whenever a container snapshot changes.
    private final java.util.List<java.util.function.BiConsumer<Integer, Integer>> inventoryListeners =
            new java.util.concurrent.CopyOnWriteArrayList<>(); // (containerId, stateId) ->
    private static final int PLAYER_INVENTORY_SIZE = 46;

    public BotClient(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.currentServer = host + ":" + port;
        // Pre-seed the player inventory so consumers can always query containerId 0.
        containers.put(0, new ContainerState(0, PLAYER_INVENTORY_SIZE, -1));
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
                    lastJoinNanos = System.nanoTime();
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
            System.out.println("[devrunner] Bot '" + name + "' SENDING command: /" + command);
            session.send(new ServerboundChatCommandPacket(command));
        } else {
            System.out.println("[devrunner] Bot '" + name + "' SENDING chat: " + message);
            // Signature MUST be null (not an empty array): Paper 26.2 reads the
            // signature as a fixed 256-byte MessageSignature blob without a
            // length prefix, so an empty array (bool=true + varint(0)) makes the
            // decoder try to consume 256 bytes and throws a DecoderException.
            // A null signature serialises to a single `false` boolean and is
            // accepted by servers with enforce-secure-profile=false.
            session.send(new ServerboundChatPacket(message, System.currentTimeMillis(), 0L, null, 0, new BitSet(), 0));
        }
    }

    public void digBlock(int x, int y, int z, int direction) {
        if (!connected.get() || session == null) {
            System.err.println("[BotClient] digBlock SKIPPED: connected=" + connected.get() + " session=" + session);
            return;
        }

        confirmPosition();

        session.send(new ServerboundSwingPacket(Hand.MAIN_HAND));

        Vector3i pos = Vector3i.from(x, y, z);
        Direction dir = Direction.values()[direction];

        int startSeq = interactionSequence.getAndIncrement();
        session.send(new ServerboundPlayerActionPacket(
                PlayerAction.START_DIGGING,
                pos,
                dir,
                startSeq
        ));

        int finishSeq = interactionSequence.getAndIncrement();
        session.send(new ServerboundPlayerActionPacket(
                PlayerAction.FINISH_DIGGING,
                pos,
                dir,
                finishSeq
        ));
    }

    public void useItemOn(int x, int y, int z, int hand, int direction) {
        if (!connected.get() || session == null) {
            System.out.println("[devrunner] Bot '" + name + "' useItemOn SKIPPED (not connected)");
            return;
        }
        Runnable action = () -> {
            if (!connected.get() || session == null) return;
            confirmPosition();

            session.send(new ServerboundSwingPacket(Hand.MAIN_HAND));

            Vector3i pos = Vector3i.from(x, y, z);
            Direction dir = Direction.values()[direction];
            Hand handEnum = hand == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;

            int seq = interactionSequence.getAndIncrement();

            System.out.println("[devrunner] Bot '" + name + "' USE-ITEM-ON at " + x + "," + y + "," + z
                    + " face=" + dir + " hand=" + handEnum + " (dead=" + dead + ")");
            session.send(new ServerboundUseItemOnPacket(
                    pos,
                    dir,
                    handEnum,
                    0.5f,
                    0.5f,
                    0.5f,
                    false,
                    false,
                    seq
            ));
        };
        runWhenClientReady(action);
    }

    public void attackEntity(int entityId) {
        if (!connected.get() || session == null) return;
        session.send(new ServerboundAttackPacket(entityId));
        session.send(new ServerboundSwingPacket(Hand.MAIN_HAND));
    }

    public void interactEntity(int entityId, int hand) {
        if (!connected.get() || session == null) return;
        Runnable action = () -> {
            if (!connected.get() || session == null) return;
            Hand handEnum = hand == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
            System.out.println("[devrunner] Bot '" + name + "' INTERACT-ENTITY " + entityId
                    + " hand=" + handEnum + " sneak=" + sneaking + " (dead=" + dead + ")");
            // The packet's sneak flag overwrites the player's shift state
            // server-side, so it must mirror the bot's current sneaking state
            // (otherwise the server would silently un-sneak the bot and
            // sneak-gated interactions would never fire).
            //
            // The location must never be null: the 26.2 protocol serialises it
            // unconditionally (length-prefixed Vec3) and the server's
            // PlayerInteractAtEntityEvent construction dereferences it, so a
            // null would NPE the client-side encoder and silently drop the
            // packet. Use the tracked entity position, falling back to the
            // bot's own position.
            org.cloudburstmc.math.vector.Vector3d location;
            EntitySnapshot snap = entitySnapshots.get(entityId);
            if (snap != null) {
                location = org.cloudburstmc.math.vector.Vector3d.from(snap.x, snap.y, snap.z);
            } else {
                location = org.cloudburstmc.math.vector.Vector3d.from(x, y, z);
            }
            session.send(new ServerboundInteractPacket(entityId, handEnum, location, sneaking));
            session.send(new ServerboundSwingPacket(handEnum));
        };
        runWhenClientReady(action);
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

    public boolean isConnected() {
        return connected.get() && session != null;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public String getName() {
        return name;
    }

    public List<String> getChatMessages() {
        List<String> messages = new ArrayList<>(chatMessages);
        chatMessages.clear();
        return messages;
    }

    public void clearChatMessages() {
        chatMessages.clear();
    }

    public void addChatCallback(Consumer<String> callback) {
        chatCallbacks.add(callback);
    }

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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getHealth() {
        return health;
    }

    public int getFood() {
        return food;
    }

    public float getSaturation() {
        return saturation;
    }

    public int getHeldItemSlot() {
        return heldItemSlot;
    }

    public String getHeldItemMaterial() {
        return heldItemMaterial;
    }

    public String getGameMode() {
        return gameMode;
    }

    public int getOpenContainerId() {
        return openContainerId;
    }

    public int getBotEntityId() {
        return botEntityId;
    }

    /**
     * Returns the latest snapshot for every tracked entity, sorted by entity id
     * (lowest first) for deterministic ordering. The returned list is a live
     * snapshot copied at call time.
     */
    public java.util.List<EntitySnapshot> entitySnapshots() {
        java.util.List<java.util.Map.Entry<Integer, EntitySnapshot>> sorted =
                new java.util.ArrayList<>(entitySnapshots.entrySet());
        sorted.sort(java.util.Map.Entry.comparingByKey());
        java.util.List<EntitySnapshot> out = new java.util.ArrayList<>(sorted.size());
        for (java.util.Map.Entry<Integer, EntitySnapshot> e : sorted) out.add(e.getValue());
        return out;
    }

    /**
     * Returns the latest snapshot for the given entity id, or {@code null} if untracked.
     */
    public EntitySnapshot entitySnapshot(int entityId) {
        return entitySnapshots.get(entityId);
    }

    /**
     * Registers a listener fired whenever any entity snapshot changes.
     */
    public void addEntityListener(Runnable listener) {
        entityListeners.add(listener);
    }

    /**
     * Registers a listener fired on every player-abilities update.
     */
    public void addAbilitiesListener(java.util.function.Consumer<AbilitiesSnapshot> listener) {
        abilitiesListeners.add(listener);
    }

    /**
     * Returns a snapshot copy of the accumulated block change snapshots.
     */
    public java.util.List<BlockSnapshot> blockSnapshots() {
        return new java.util.ArrayList<>(blockSnapshots);
    }

    /**
     * Clears all accumulated block snapshots.
     */
    public void clearBlockSnapshots() {
        blockSnapshots.clear();
    }

    /**
     * Registers a listener fired whenever block snapshots are added.
     */
    public void addBlockListener(Runnable listener) {
        blockListeners.add(listener);
    }

    /**
     * Returns the latest cached explosion snapshot, or {@code null} if none has arrived yet.
     */
    public ExplosionSnapshot latestExplosion() {
        return latestExplosion;
    }

    /**
     * Registers a listener fired whenever an explosion is received.
     */
    public void addExplosionListener(Runnable listener) {
        explosionListeners.add(listener);
    }

    private void fireExplosionChanged() {
        for (Runnable l : explosionListeners) {
            try {
                l.run();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Returns the latest cached abilities, or {@code null} if none has arrived yet.
     */
    public AbilitiesSnapshot abilities() {
        return abilities;
    }

    /**
     * Requests tab-completion suggestions for the given text and returns a
     * future completed with the server's response (or timed out).
     */
    public java.util.concurrent.CompletableFuture<java.util.List<Suggestion>> queryTabComplete(String text) {
        if (!connected.get() || session == null) {
            return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("Bot '" + name + "' is not connected"));
        }
        int txn = suggestionTxnCounter.incrementAndGet();
        java.util.concurrent.CompletableFuture<java.util.List<Suggestion>> future = new java.util.concurrent.CompletableFuture<>();
        pendingSuggestions.put(txn, future);
        try {
            session.send(new ServerboundCommandSuggestionPacket(txn, text));
        } catch (RuntimeException e) {
            pendingSuggestions.remove(txn);
            return java.util.concurrent.CompletableFuture.failedFuture(e);
        }
        // 30s safety timeout - server may silently drop the request.
        io.netty.channel.Channel ch = session.getChannel();
        if (ch != null && ch.eventLoop() != null) {
            ch.eventLoop().schedule(() -> {
                java.util.concurrent.CompletableFuture<java.util.List<Suggestion>> f = pendingSuggestions.remove(txn);
                if (f != null) f.completeExceptionally(new java.util.concurrent.TimeoutException(
                        "Tab-completion for '" + name + "' timed out (txn=" + txn + ")"));
            }, 30, TimeUnit.SECONDS);
        }
        return future;
    }

    /**
     * Sends a request to respawn after death (no effect if the bot is alive).
     */
    public void respawn() {
        if (!connected.get() || session == null) return;
        session.send(new ServerboundClientCommandPacket(ClientCommand.PERFORM_RESPAWN));
    }

    /**
     * Sends a respawn request when the bot is dead and re-arms the check a
     * second later, so a respawn that races the death-screen setup is retried.
     */
    private void requestRespawn() {
        if (!connected.get() || session == null || !dead) return;
        session.send(new ServerboundClientCommandPacket(ClientCommand.PERFORM_RESPAWN));
        int attempts = respawnAttempts.incrementAndGet();
        if (attempts >= 5) {
            return; // give up after repeated refusals; avoid an infinite retry loop
        }
        io.netty.channel.Channel ch = session.getChannel();
        if (ch != null && ch.eventLoop() != null) {
            ch.eventLoop().schedule(() -> {
                if (dead && connected.get() && respawnAttempts.get() < 5) {
                    requestRespawn();
                }
            }, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Sends the bot's current input flags via {@code ServerboundPlayerInputPacket}.
     * In 1.21+ this is how sneak/sprint is communicated (no longer via
     * {@code ServerboundPlayerCommandPacket}).
     */
    public void sendPlayerInput(boolean forward, boolean backward, boolean left, boolean right,
                                boolean jump, boolean sneak, boolean sprint) {
        if (!connected.get() || session == null) return;
        this.sneaking = sneak;
        session.send(new ServerboundPlayerInputPacket(forward, backward, left, right, jump, sneak, sprint));
    }

    /**
     * Toggles the player's flying flag (only meaningful if the server allows it).
     */
    public void setFlying(boolean enable) {
        if (!connected.get() || session == null) return;
        session.send(new ServerboundPlayerAbilitiesPacket(enable));
    }

    /**
     * Uses the currently held item (right-click in air). {@code hand} = 0 for main, 1 for off.
     */
    public void useItem(int hand) {
        if (!connected.get() || session == null) return;
        Runnable action = () -> {
            if (!connected.get() || session == null) return;
            confirmPosition();
            Hand handEnum = hand == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
            int seq = actionSequence++;
            System.out.println("[devrunner] Bot '" + name + "' USE-ITEM hand=" + handEnum
                    + " (dead=" + dead + ")");
            session.send(new ServerboundUseItemPacket(handEnum, seq, yaw, pitch));
        };
        runWhenClientReady(action);
    }

    /**
     * Executes a physical action immediately, or holds it until the server's
     * "client loaded" window has elapsed. Paper 26.x runs a ~3s timeout after
     * a player joins and restarts it after every respawn; while it runs, the
     * server silently drops every use/use-on/interact packet
     * ({@code hasClientLoaded()} is false).
     *
     * <p>The {@link #lastJoinNanos}/{@link #lastRespawnNanos} sentinels are
     * {@code Long.MIN_VALUE} until the corresponding server event was seen.
     * {@code nanoTime() - Long.MIN_VALUE} overflows and wraps negative, which
     * would turn {@code waitMs} into a huge positive value and schedule the
     * action ~292 years in the future - silently dropping every physical
     * action. Sentinels are treated as "no window to wait for".</p>
     */
    private void runWhenClientReady(Runnable action) {
        long nowNanos = System.nanoTime();
        long waitMs = 0;
        if (lastJoinNanos != Long.MIN_VALUE) {
            long elapsedSinceJoin = (nowNanos - lastJoinNanos) / 1_000_000L;
            waitMs = Math.max(waitMs, CLIENT_READY_DELAY_MS - elapsedSinceJoin);
        }
        if (lastRespawnNanos != Long.MIN_VALUE) {
            long elapsedSinceRespawn = (nowNanos - lastRespawnNanos) / 1_000_000L;
            waitMs = Math.max(waitMs, CLIENT_READY_DELAY_MS - elapsedSinceRespawn);
        }
        if (waitMs <= 0 || session == null) {
            action.run();
            return;
        }
        io.netty.channel.Channel ch = session.getChannel();
        if (ch == null || ch.eventLoop() == null) {
            action.run();
            return;
        }
        ch.eventLoop().schedule(action, waitMs, TimeUnit.MILLISECONDS);
    }

    public void moveTo(double targetX, double targetY, double targetZ) {
        if (!connected.get() || session == null || !hasPosition) return;
        System.out.println("[devrunner] Bot '" + name + "' MOVE to " + targetX + "," + targetY + "," + targetZ
                + " (connected=" + connected.get() + ")");
        session.send(new ServerboundMovePlayerPosRotPacket(true, false, targetX, targetY, targetZ, yaw, pitch));
    }

    /**
     * Sends a short movement update confirming the bot's current position.
     * Paper's anti-cheat silently rejects actions (dig, interact, use) from
     * clients that have not recently confirmed their position via a movement
     * packet.  Calling this before every player-initiated action ensures the
     * server accepts it.
     *
     * <p>No-op until the server has reported a position (see {@link #hasPosition}) -
     * sending the initial (0,0,0) cache would trigger a fail-move instead.</p>
     */
    private void confirmPosition() {
        if (!connected.get() || session == null || !hasPosition) return;
        session.send(new ServerboundMovePlayerPosRotPacket(true, false, x, y, z, yaw, pitch));
    }

    // ── Inventory ──────────────────────────────────────────────────────────

    /**
     * Maps an {@code ClientboundOpenScreenPacket} menu type to its container
     * slot count (excluding the trailing player-inventory section that the
     * server appends to {@code ClientboundContainerSetContentPacket} on
     * 1.21.2+). Returns {@code -1} for unknown types - the snapshot then
     * keeps whatever the content packet reported.
     */
    private static int containerSizeForType(ContainerType type) {
        if (type == null) return -1;
        return switch (type) {
            case GENERIC_9X1 -> 9;
            case GENERIC_9X2 -> 18;
            case GENERIC_9X3 -> 27;
            case GENERIC_9X4 -> 36;
            case GENERIC_9X5 -> 45;
            case GENERIC_9X6 -> 54;
            case GENERIC_3X3 -> 9;
            case CRAFTER_3x3 -> 9;
            case ANVIL -> 3;
            case BEACON -> 1;
            case BLAST_FURNACE -> 3;
            case BREWING_STAND -> 5;
            case CRAFTING -> 0;
            case ENCHANTMENT -> 2;
            case FURNACE -> 3;
            case GRINDSTONE -> 3;
            case HOPPER -> 5;
            case LECTERN -> 1;
            case LOOM -> 4;
            case MERCHANT -> 3;
            case SHULKER_BOX -> 27;
            case SMITHING -> 4;
            case SMOKER -> 3;
            case CARTOGRAPHY -> 3;
            case STONECUTTER -> 2;
            default -> -1;
        };
    }

    /**
     * Builds a snapshot of the container with the given id. Returns {@code null}
     * if the bot has never received a content/slot packet for that container
     * (other than the always-present player inventory at containerId 0).
     */
    public ContainerSnapshot snapshotInventory(int containerId) {
        ContainerState st = containers.get(containerId);
        if (st == null) return null;
        return st.toSnapshot();
    }

    /**
     * Adds a listener invoked whenever a container snapshot changes.
     */
    public void addInventoryListener(java.util.function.BiConsumer<Integer, Integer> listener) {
        inventoryListeners.add(listener);
    }

    private void fireInventoryChanged(int containerId) {
        ContainerState st = containers.get(containerId);
        int stateId = st != null ? st.stateId : 0;
        for (java.util.function.BiConsumer<Integer, Integer> l : inventoryListeners) {
            try {
                l.accept(containerId, stateId);
            } catch (Exception ignored) {
            }
        }
    }

    private void fireEntityChanged() {
        for (Runnable l : entityListeners) {
            try {
                l.run();
            } catch (Exception ignored) {
            }
        }
    }

    private void fireBlockChanged() {
        for (Runnable l : blockListeners) {
            try {
                l.run();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Trims the block snapshots list to {@link #MAX_BLOCK_SNAPSHOTS} entries,
     * removing the oldest ones from the front.
     */
    private void trimBlockSnapshots() {
        while (blockSnapshots.size() > MAX_BLOCK_SNAPSHOTS) {
            blockSnapshots.remove(0);
        }
    }

    /**
     * Performs a click in a container slot. {@code clickTypeName} is one of the
     * names defined by {@code de.t14d3.rapunzellib.livetest.BotInventory.ClickType}
     * - the gradle-plugin avoids a hard dep on the livetest module, so the
     * caller passes the enum's {@code name()}.
     */
    public void clickContainer(int containerId, int slot, int button, String clickTypeName) {
        if (!connected.get() || session == null) return;
        ContainerState st = containers.get(containerId);
        int stateId = st != null ? st.stateId : 0;
        ContainerActionType actionType;
        ContainerAction param;
        switch (clickTypeName) {
            case "LEFT_CLICK" -> {
                actionType = ContainerActionType.CLICK_ITEM;
                param = ClickItemAction.LEFT_CLICK;
            }
            case "RIGHT_CLICK" -> {
                actionType = ContainerActionType.CLICK_ITEM;
                param = ClickItemAction.RIGHT_CLICK;
            }
            case "SHIFT_CLICK" -> {
                actionType = ContainerActionType.SHIFT_CLICK_ITEM;
                param = button == 1 ? ShiftClickItemAction.RIGHT_CLICK : ShiftClickItemAction.LEFT_CLICK;
            }
            case "HOTBAR_SWAP" -> {
                actionType = ContainerActionType.MOVE_TO_HOTBAR_SLOT;
                param = MoveToHotbarAction.from(button);
            }
            case "CREATIVE_GRAB" -> {
                actionType = ContainerActionType.CREATIVE_GRAB_MAX_STACK;
                param = org.geysermc.mcprotocollib.protocol.data.game.inventory.CreativeGrabAction.from(button);
            }
            case "DROP_ONE" -> {
                actionType = ContainerActionType.DROP_ITEM;
                param = DropItemAction.DROP_FROM_SELECTED;
            }
            case "DROP_ALL" -> {
                actionType = ContainerActionType.DROP_ITEM;
                param = DropItemAction.DROP_SELECTED_STACK;
            }
            default -> throw new IllegalArgumentException("Unknown click type: " + clickTypeName);
        }
        HashedStack carried = null;
        it.unimi.dsi.fastutil.ints.Int2ObjectMap<HashedStack> changed =
                new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
        session.send(new ServerboundContainerClickPacket(
                containerId, stateId, slot, actionType, param, carried, changed));
    }

    /**
     * Sends a close-container packet for the given container id.
     */
    public void closeContainer(int containerId) {
        if (!connected.get() || session == null) return;
        session.send(new ServerboundContainerClosePacket(containerId));
        if (containerId != 0) containers.remove(containerId);
        if (openContainerId == containerId) openContainerId = -1;
    }

    /**
     * Drops the bot's currently held stack. The slot number is left at
     * {@code -999} (which means "the active hotbar slot") per the protocol
     * convention, and the action type is {@code DROP_ITEM} with either
     * {@code DROP_FROM_SELECTED} (drop one) or {@code DROP_SELECTED_STACK}
     * (drop all).
     */
    public void dropHeldItem(boolean dropAll) {
        if (!connected.get() || session == null) return;
        ContainerState st = containers.get(0);
        int stateId = st != null ? st.stateId : 0;
        DropItemAction action = dropAll ? DropItemAction.DROP_SELECTED_STACK : DropItemAction.DROP_FROM_SELECTED;
        HashedStack carried = null;
        it.unimi.dsi.fastutil.ints.Int2ObjectMap<HashedStack> changed =
                new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
        // Use slot -999 which is the special "outside" slot that triggers a
        // drop-from-selected action - required by the packet's invariants.
        session.send(new ServerboundContainerClickPacket(
                0, stateId, ServerboundContainerClickPacket.CLICK_OUTSIDE_NOT_HOLDING_SLOT,
                ContainerActionType.DROP_ITEM, action, carried, changed));
    }

    /**
     * Places an item into a slot using creative mode. No effect outside creative.
     */
    public void setCreativeSlot(int slot, int itemId, int amount) {
        if (!connected.get() || session == null) return;
        ItemStack stack = (itemId < 0 || amount <= 0) ? null : new ItemStack(itemId, amount, null);
        session.send(new ServerboundSetCreativeModeSlotPacket((short) slot, stack));
    }

    /**
     * Snapshot returned to the {@code BotTcpServer}.
     */
    public static final class ContainerSnapshot {
        public final int containerId;
        public final int stateId;
        public final ItemStack[] slots;
        public final ItemStack cursorItem;

        public ContainerSnapshot(int containerId, int stateId, ItemStack[] slots, ItemStack cursorItem) {
            this.containerId = containerId;
            this.stateId = stateId;
            this.slots = slots;
            this.cursorItem = cursorItem;
        }
    }

    /**
     * Mutable per-container state. All mutations happen on the network thread.
     */
    private static final class ContainerState {
        final int containerId;
        /** Expected container slot count from the OpenScreen menu type, or -1 if unknown. */
        final int containerSize;
        int stateId;
        ItemStack[] slots;
        ItemStack cursorItem;
        final java.util.Map<Integer, Integer> properties = new java.util.HashMap<>();

        ContainerState(int containerId, int initialSize, int containerSize) {
            this.containerId = containerId;
            this.containerSize = containerSize;
            this.slots = new ItemStack[Math.max(initialSize, 0)];
            this.cursorItem = null;
        }

        synchronized void applyFull(int stateId, @Nullable ItemStack[] incoming, @Nullable ItemStack cursor) {
            this.stateId = stateId;
            int len = incoming != null ? incoming.length : 0;
            // Player inventory never shrinks below the standard size.
            int newSize;
            if (containerId != 0 && containerSize > 0) {
                // 1.21.2+ SetContent packets append the player-inventory
                // section after the container's own slots (e.g. 27 + 36 for a
                // chest menu); the container snapshot must only expose the
                // container's section.
                len = Math.min(len, containerSize);
                newSize = len;
            } else {
                newSize = containerId == 0 ? Math.max(PLAYER_INVENTORY_SIZE, len) : len;
            }
            ItemStack[] fresh = new ItemStack[newSize];
            int copy = Math.min(len, newSize);
            for (int i = 0; i < copy; i++) fresh[i] = incoming[i];
            this.slots = fresh;
            if (cursor != null || containerId == 0) this.cursorItem = cursor;
        }

        synchronized void applySlot(int incomingStateId, int slot, @Nullable ItemStack item) {
            if (incomingStateId != this.stateId) {
                // Server may fast-forward the state id without resyncing the
                // full content. We accept the new state id only when the
                // requested slot fits.
                this.stateId = incomingStateId;
            }
            ensureSize(slot + 1);
            this.slots[slot] = item;
        }

        synchronized void applySlotUnknownState(int slot, @Nullable ItemStack item) {
            ensureSize(slot + 1);
            this.slots[slot] = item;
        }

        synchronized void setCursor(@Nullable ItemStack cursor) {
            this.cursorItem = cursor;
        }

        private void ensureSize(int minSize) {
            if (slots == null) {
                slots = new ItemStack[Math.max(PLAYER_INVENTORY_SIZE, minSize)];
                return;
            }
            int cap = containerId == 0 ? Math.max(PLAYER_INVENTORY_SIZE, minSize) : minSize;
            if (slots.length < cap) {
                ItemStack[] grown = new ItemStack[cap];
                System.arraycopy(slots, 0, grown, 0, slots.length);
                slots = grown;
            }
        }

        synchronized void setProperty(int id, int value) {
            properties.put(id, value);
        }

        synchronized ContainerSnapshot toSnapshot() {
            // Defensive copy. For open containers with a known menu size, only
            // the container's own slots are exposed (the buffer may also hold
            // trailing player-inventory slots received on 1.21.2+).
            int cap = slots != null ? slots.length : 0;
            if (containerId != 0 && containerSize > 0) {
                cap = Math.min(cap, containerSize);
            }
            ItemStack[] copy = new ItemStack[cap];
            if (slots != null) System.arraycopy(slots, 0, copy, 0, cap);
            return new ContainerSnapshot(containerId, stateId, copy, cursorItem);
        }
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
                hasPosition = true;
                session.send(new ServerboundAcceptTeleportationPacket(posPacket.getId()));
                return;
            }
            if (packet instanceof ClientboundSetHealthPacket healthPacket) {
                health = healthPacket.getHealth();
                food = healthPacket.getFood();
                saturation = healthPacket.getSaturation();
                if (health <= 0.0f) {
                    // Dead players are immobile on the server: every use/attack
                    // packet is silently dropped. Auto-respawn so the bot keeps
                    // working (also covers rejoining with persisted health=0).
                    if (!dead) {
                        dead = true;
                        respawnAttempts.set(0);
                        System.out.println("[devrunner] Bot '" + name + "' DIED (health=0), requesting respawn");
                    }
                    requestRespawn();
                } else if (dead) {
                    dead = false;
                    respawnAttempts.set(0);
                    lastRespawnNanos = System.nanoTime();
                    System.out.println("[devrunner] Bot '" + name + "' respawned, holding physical actions "
                            + CLIENT_READY_DELAY_MS + "ms");
                }
                return;
            }
            if (packet instanceof ClientboundOpenScreenPacket openScreen) {
                openContainerId = openScreen.getContainerId();
                int size = containerSizeForType(openScreen.getType());
                containers.put(openContainerId, new ContainerState(openContainerId, Math.max(size, 0), size));
                fireInventoryChanged(openContainerId);
                return;
            }
            if (packet instanceof ClientboundContainerClosePacket closeContainer) {
                int cid = closeContainer.getContainerId();
                if (cid != 0) containers.remove(cid);
                if (cid == openContainerId) openContainerId = -1;
                if (cid == 0) {
                    // Closing the player inventory view isn't a real thing -
                    // the player inventory is always tracked at containerId 0.
                    containers.putIfAbsent(0, new ContainerState(0, PLAYER_INVENTORY_SIZE, -1));
                }
                fireInventoryChanged(0);
                return;
            }
            if (packet instanceof ClientboundContainerSetContentPacket content) {
                int cid = content.getContainerId();
                ContainerState st = containers.computeIfAbsent(cid,
                        k -> new ContainerState(cid, content.getItems() != null ? content.getItems().length : PLAYER_INVENTORY_SIZE, -1));
                st.applyFull(content.getStateId(), content.getItems(), content.getCarriedItem());
                cursorItemContainerId = cid;
                fireInventoryChanged(cid);
                return;
            }
            if (packet instanceof ClientboundContainerSetSlotPacket slotPacket) {
                int cid = slotPacket.getContainerId();
                ContainerState st = containers.computeIfAbsent(cid,
                        k -> new ContainerState(cid, PLAYER_INVENTORY_SIZE, -1));
                st.applySlot(slotPacket.getStateId(), slotPacket.getSlot(), slotPacket.getItem());
                if (slotPacket.getSlot() < 0 && slotPacket.getItem() != null) {
                    st.setCursor(slotPacket.getItem());
                    cursorItemContainerId = cid;
                }
                fireInventoryChanged(cid);
                return;
            }
            if (packet instanceof ClientboundSetCursorItemPacket cursor) {
                ContainerState st = containers.get(cursorItemContainerId);
                if (st == null) st = containers.computeIfAbsent(0,
                        k -> new ContainerState(0, PLAYER_INVENTORY_SIZE, -1));
                st.setCursor(cursor.getContents());
                fireInventoryChanged(st.containerId);
                return;
            }
            if (packet instanceof ClientboundContainerSetDataPacket data) {
                ContainerState st = containers.get(data.getContainerId());
                if (st != null) {
                    st.setProperty(data.getRawProperty(), data.getValue());
                    fireInventoryChanged(st.containerId);
                }
                return;
            }
            if (packet instanceof ClientboundSetPlayerInventoryPacket invSlot) {
                ContainerState st = containers.computeIfAbsent(0,
                        k -> new ContainerState(0, PLAYER_INVENTORY_SIZE, -1));
                st.applySlotUnknownState(invSlot.getSlot(), invSlot.getContents());
                fireInventoryChanged(0);
                return;
            }
            if (packet instanceof ClientboundAddEntityPacket addEntity) {
                int id = addEntity.getEntityId();
                String typeName = addEntity.getType() != null ? addEntity.getType().name() : "unknown";
                int typeId = addEntity.getType() != null ? addEntity.getType().ordinal() : -1;
                EntitySnapshot snap = new EntitySnapshot(
                        id, typeId, typeName,
                        addEntity.getX(), addEntity.getY(), addEntity.getZ(),
                        addEntity.getYaw(), addEntity.getHeadYaw(), addEntity.getPitch());
                entitySnapshots.put(id, snap);
                fireEntityChanged();
                return;
            }
            if (packet instanceof ClientboundMoveEntityPosPacket movePos) {
                EntitySnapshot prev = entitySnapshots.get(movePos.getEntityId());
                if (prev != null) {
                    entitySnapshots.put(movePos.getEntityId(), prev.withPosition(
                            prev.x + movePos.getMoveX(),
                            prev.y + movePos.getMoveY(),
                            prev.z + movePos.getMoveZ()));
                    fireEntityChanged();
                }
                return;
            }
            if (packet instanceof ClientboundMoveEntityPosRotPacket movePosRot) {
                EntitySnapshot prev = entitySnapshots.get(movePosRot.getEntityId());
                if (prev != null) {
                    entitySnapshots.put(movePosRot.getEntityId(), prev.withPositionAndRotation(
                            prev.x + movePosRot.getMoveX(),
                            prev.y + movePosRot.getMoveY(),
                            prev.z + movePosRot.getMoveZ(),
                            movePosRot.getYaw(), movePosRot.getPitch()));
                    fireEntityChanged();
                }
                return;
            }
            if (packet instanceof ClientboundMoveEntityRotPacket moveRot) {
                EntitySnapshot prev = entitySnapshots.get(moveRot.getEntityId());
                if (prev != null) {
                    entitySnapshots.put(moveRot.getEntityId(), prev.withRotation(moveRot.getYaw(), moveRot.getPitch()));
                    fireEntityChanged();
                }
                return;
            }
            if (packet instanceof ClientboundTeleportEntityPacket teleport) {
                int id = teleport.getId();
                EntitySnapshot prev = entitySnapshots.get(id);
                EntitySnapshot fresh = (prev != null ? prev : new EntitySnapshot(id, -1, "unknown", 0, 0, 0, 0, 0, 0))
                        .withPositionAndRotation(
                                teleport.getPosition().getX(),
                                teleport.getPosition().getY(),
                                teleport.getPosition().getZ(),
                                teleport.getYRot(), teleport.getXRot());
                entitySnapshots.put(id, fresh);
                fireEntityChanged();
                return;
            }
            if (packet instanceof ClientboundEntityPositionSyncPacket sync) {
                int id = sync.getId();
                EntitySnapshot prev = entitySnapshots.get(id);
                EntitySnapshot fresh = (prev != null ? prev : new EntitySnapshot(id, -1, "unknown", 0, 0, 0, 0, 0, 0))
                        .withPositionAndRotation(
                                sync.getPosition().getX(),
                                sync.getPosition().getY(),
                                sync.getPosition().getZ(),
                                sync.getYRot(), sync.getXRot());
                entitySnapshots.put(id, fresh);
                fireEntityChanged();
                return;
            }
            if (packet instanceof ClientboundRemoveEntitiesPacket removeEntities) {
                boolean changed = false;
                for (int id : removeEntities.getEntityIds()) {
                    if (entitySnapshots.remove(id) != null) changed = true;
                }
                if (changed) fireEntityChanged();
                return;
            }
            if (packet instanceof ClientboundBlockUpdatePacket blockUpdate) {
                Vector3i pos = blockUpdate.getEntry().getPosition();
                int stateId = blockUpdate.getEntry().getBlock();
                blockSnapshots.add(new BlockSnapshot(pos.getX(), pos.getY(), pos.getZ(), stateId));
                trimBlockSnapshots();
                fireBlockChanged();
                return;
            }
            if (packet instanceof ClientboundSectionBlocksUpdatePacket sectionUpdate) {
                boolean added = false;
                for (org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry entry : sectionUpdate.getEntries()) {
                    Vector3i pos = entry.getPosition();
                    int stateId = entry.getBlock();
                    blockSnapshots.add(new BlockSnapshot(pos.getX(), pos.getY(), pos.getZ(), stateId));
                    added = true;
                }
                if (added) {
                    trimBlockSnapshots();
                    fireBlockChanged();
                }
                return;
            }
            if (packet instanceof ClientboundExplodePacket explode) {
                latestExplosion = new ExplosionSnapshot(
                        explode.getCenter().getX(), explode.getCenter().getY(), explode.getCenter().getZ(),
                        explode.getRadius());
                fireExplosionChanged();
                return;
            }
            if (packet instanceof ClientboundPlayerAbilitiesPacket abilitiesPacket) {
                // Guard against stale abilities packets: if our abilities were already synthesised from
                // a game-mode change, don't overwrite them with the delayed survival defaults.
                boolean packetCreative = abilitiesPacket.isCreative();
                boolean packetCanFly = abilitiesPacket.isCanFly();
                boolean modeIsCreative = "creative".equals(gameMode);
                boolean modeIsSpectator = "spectator".equals(gameMode);
                if ((modeIsCreative || modeIsSpectator) == packetCreative
                        || abilities == null) {
                    abilities = new AbilitiesSnapshot(
                            abilitiesPacket.isInvincible(),
                            packetCanFly,
                            abilitiesPacket.isFlying(),
                            packetCreative,
                            abilitiesPacket.getFlySpeed(),
                            abilitiesPacket.getWalkSpeed());
                }
                // Always fire the listener so downstream code sees the
                // event, even if we chose not to replace `abilities`.
                // (The RpcBotService cache is updated from every event.)
                for (java.util.function.Consumer<AbilitiesSnapshot> l : abilitiesListeners) {
                    try {
                        l.accept(abilities);
                    } catch (Exception ignored) {
                    }
                }
                return;
            }
            if (packet instanceof ClientboundCommandSuggestionsPacket suggestions) {
                int txn = suggestions.getTransactionId();
                java.util.concurrent.CompletableFuture<java.util.List<Suggestion>> future = pendingSuggestions.remove(txn);
                if (future != null) {
                    String[] matches = suggestions.getMatches();
                    net.kyori.adventure.text.Component[] tooltips = suggestions.getTooltips();
                    java.util.List<Suggestion> out = new java.util.ArrayList<>(matches.length);
                    for (int i = 0; i < matches.length; i++) {
                        String tooltip = (i < tooltips.length && tooltips[i] != null) ? serializeComponent(tooltips[i]) : null;
                        out.add(new Suggestion(matches[i], tooltip));
                    }
                    future.complete(java.util.Collections.unmodifiableList(out));
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
                        // The server doesn't resend ClientboundPlayerAbilitiesPacket on game-mode changes,
                        // so synthesise an abilities update from ClientboundGameEventPacket.
                        boolean isCreative = mode == GameMode.CREATIVE;
                        boolean isSpectator = mode == GameMode.SPECTATOR;
                        boolean canFly = isCreative || isSpectator;
                        boolean invincible = isCreative || isSpectator;
                        AbilitiesSnapshot synthesized = new AbilitiesSnapshot(
                                invincible, canFly, false, isCreative,
                                0.05f, 0.1f);
                        abilities = synthesized;
                        for (java.util.function.Consumer<AbilitiesSnapshot> l : abilitiesListeners) {
                            try {
                                l.accept(abilities);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
                return;
            }
            String message = extractChatMessage(packet);
            if (message != null) {
                System.out.println("[devrunner] Bot '" + name + "' CHAT RECEIVED: " + message);
                chatMessages.add(message);
                for (Consumer<String> callback : chatCallbacks) {
                    try {
                        callback.accept(message);
                    } catch (Exception ignored) {
                    }
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
            try {
                return PlainTextComponentSerializer.plainText().serialize(component);
            } catch (Exception e) {
                return component.toString();
            }
        }
    }

    // ── Snapshot types exposed by BotClient ────────────────────────────────

    /**
     * Immutable snapshot of a tracked entity. Position fields are absolute.
     */
    public static final class EntitySnapshot {
        public final int entityId;
        public final int typeId;
        public final String typeName;
        public final double x, y, z;
        public final float yaw, headYaw, pitch;

        public EntitySnapshot(int entityId, int typeId, String typeName,
                              double x, double y, double z,
                              float yaw, float headYaw, float pitch) {
            this.entityId = entityId;
            this.typeId = typeId;
            this.typeName = typeName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.headYaw = headYaw;
            this.pitch = pitch;
        }

        EntitySnapshot withPosition(double nx, double ny, double nz) {
            return new EntitySnapshot(entityId, typeId, typeName, nx, ny, nz, yaw, headYaw, pitch);
        }

        EntitySnapshot withRotation(float nyaw, float npitch) {
            return new EntitySnapshot(entityId, typeId, typeName, x, y, z, nyaw, headYaw, npitch);
        }

        EntitySnapshot withPositionAndRotation(double nx, double ny, double nz, float nyaw, float npitch) {
            return new EntitySnapshot(entityId, typeId, typeName, nx, ny, nz, nyaw, headYaw, npitch);
        }
    }

    /**
     * Immutable snapshot of a block change reported by the server.
     */
    public static record BlockSnapshot(int x, int y, int z, int blockStateId) {
    }

    /**
     * Immutable snapshot of an explosion event reported by the server.
     */
    public static record ExplosionSnapshot(double x, double y, double z, float radius) {
    }

    /**
     * Immutable snapshot of the bot's player abilities.
     */
    public static final class AbilitiesSnapshot {
        public final boolean invincible;
        public final boolean canFly;
        public final boolean flying;
        public final boolean creative;
        public final float flySpeed;
        public final float walkSpeed;

        public AbilitiesSnapshot(boolean invincible, boolean canFly, boolean flying, boolean creative,
                                 float flySpeed, float walkSpeed) {
            this.invincible = invincible;
            this.canFly = canFly;
            this.flying = flying;
            this.creative = creative;
            this.flySpeed = flySpeed;
            this.walkSpeed = walkSpeed;
        }
    }

    /**
     * Immutable single tab-completion suggestion.
     */
    public static final class Suggestion {
        public final String match;
        public final String tooltip; // nullable

        public Suggestion(String match, String tooltip) {
            this.match = match;
            this.tooltip = tooltip;
        }
    }
}