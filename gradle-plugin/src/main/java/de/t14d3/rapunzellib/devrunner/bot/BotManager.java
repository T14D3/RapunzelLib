package de.t14d3.rapunzellib.devrunner.bot;

import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Manages multiple bot connections for the DevRunner.
 *
 * <p>Also exposes a way to push server-side events (chat arrivals, disconnects,
 * server transfers) that the {@link BotTcpServer} surfaces to connected
 * {@code RpcBotService} clients.</p>
 */
public class BotManager {

    private final Map<String, BotClient> bots = new ConcurrentHashMap<>();

    /** Listener fired when a bot receives a chat message. (botName, message) */
    private volatile BiConsumer<String, String> chatEventListener;
    /** Listener fired when a bot disconnects. (botName, reasonOrNull) */
    private volatile BiConsumer<String, String> disconnectListener;
    /** Listener fired when a bot's inventory updates. (botName, containerId) */
    private volatile BiConsumer<String, Integer> inventoryListener;
    /** Listener fired when a bot joins the server (post-transfer ready). (botName, serverHost) */
    private volatile BiConsumer<String, String> serverJoinListener;
    /** Listener fired when a bot's entity tracking updates. (botName) */
    private volatile BiConsumer<String, Object> entityListener; // Object = unused payload
    /** Listener fired when a bot's abilities update. (botName, snapshot) */
    private volatile BiConsumer<String, BotClient.AbilitiesSnapshot> abilitiesListener;
    /** Listener fired when a bot's block tracking updates. (botName, unused) */
    private volatile BiConsumer<String, Object> blockListener;

    /** Listener fired when a bot receives an explosion event. (botName, unused) */
    private volatile BiConsumer<String, Object> explosionListener;

    public void connectBot(String name, String host, int port) throws Exception {
        BotClient existing = bots.get(name);
        if (existing != null && existing.isConnected()) {
            System.out.println("[devrunner] Bot '" + name + "' already connected to " + host + ":" + port);
            return;
        }
        if (existing != null) {
            existing.disconnect();
            bots.remove(name);
        }
        BotClient client = new BotClient(name, host, port);
        wireEventListeners(name, client, host);
        client.connect();
        bots.put(name, client);
        System.out.println("[devrunner] Bot '" + name + "' connected to " + host + ":" + port);
    }

    private void wireEventListeners(String name, BotClient client, String host) {
        client.addChatCallback(message -> {
            BiConsumer<String, String> l = chatEventListener;
            if (l != null) l.accept(name, message);
        });
        client.addInventoryListener((containerId, stateId) -> {
            BiConsumer<String, Integer> l = inventoryListener;
            if (l != null) l.accept(name, containerId);
        });
        client.addEntityListener(() -> {
            BiConsumer<String, Object> l = entityListener;
            if (l != null) l.accept(name, null);
        });
        client.addAbilitiesListener(snap -> {
            BiConsumer<String, BotClient.AbilitiesSnapshot> l = abilitiesListener;
            if (l != null) l.accept(name, snap);
        });
        client.addBlockListener(() -> {
            BiConsumer<String, Object> l = blockListener;
            if (l != null) l.accept(name, null);
        });
        client.addExplosionListener(() -> {
            BiConsumer<String, Object> l = explosionListener;
            if (l != null) l.accept(name, null);
        });
        BiConsumer<String, String> joins = serverJoinListener;
        if (joins != null) {
            // Fire once after a successful connect - the BotClient.connect()
            // has already returned, so we notify on a worker thread.
            new Thread(() -> {
                try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                joins.accept(name, host);
            }, "bot-join-notify-" + name).start();
        }
    }

    public void disconnectBot(String name) {
        BotClient client = bots.remove(name);
        if (client != null) {
            client.disconnect();
            BiConsumer<String, String> l = disconnectListener;
            if (l != null) l.accept(name, null);
            System.out.println("[devrunner] Bot '" + name + "' disconnected");
        }
    }

    public void execute(String name, String command) {
        BotClient client = bots.get(name);
        if (client == null) { System.out.println("[devrunner] Cannot execute on bot '" + name + "': not found"); return; }
        client.sendChat(command);
        System.out.println("[devrunner] Bot '" + name + "' executing: " + command);
    }

    public BotClient getBot(String name) { return bots.get(name); }

    public boolean hasBot(String name) { return bots.containsKey(name); }

    public void digBlock(String name, int x, int y, int z, int direction) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.digBlock(x, y, z, direction);
    }

    public void useItemOn(String name, int x, int y, int z, int hand, int direction) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.useItemOn(x, y, z, hand, direction);
    }

    public void disconnectAll() {
        for (Map.Entry<String, BotClient> entry : bots.entrySet()) {
            entry.getValue().disconnect();
        }
        bots.clear();
    }

    public double[] queryPosition(String name) {
        BotClient client = bots.get(name);
        if (client == null) return null;
        return new double[]{client.getX(), client.getY(), client.getZ(), client.getYaw(), client.getPitch()};
    }

    public float[] queryHealth(String name) {
        BotClient client = bots.get(name);
        if (client == null) return null;
        return new float[]{client.getHealth(), client.getFood(), client.getSaturation()};
    }

    public int[] queryHeldItem(String name) {
        BotClient client = bots.get(name);
        if (client == null) return null;
        return new int[]{client.getHeldItemSlot()};
    }

    public String queryGameMode(String name) {
        BotClient client = bots.get(name);
        if (client == null) return "unknown";
        return client.getGameMode();
    }

    public int queryOpenContainerId(String name) {
        BotClient client = bots.get(name);
        if (client == null) return -1;
        return client.getOpenContainerId();
    }

    public int[] findEntities(String name, String typeName) {
        BotClient client = bots.get(name);
        if (client == null) return new int[0];
        String upper = typeName != null ? typeName.toUpperCase() : "";
        java.util.List<BotClient.EntitySnapshot> snaps = client.entitySnapshots();
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        for (BotClient.EntitySnapshot s : snaps) {
            String entryUpper = s.typeName != null ? s.typeName.toUpperCase() : "";
            if (entryUpper.equals(upper) || entryUpper.replace("minecraft:", "").equals(upper)) {
                ids.add(s.entityId);
            }
        }
        return ids.stream().mapToInt(i -> i).toArray();
    }

    public void moveTo(String name, int x, int y, int z) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.moveTo(x, y, z);
    }

    public void attackEntity(String name, int entityId) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.attackEntity(entityId);
    }

    public void interactEntity(String name, int entityId, int hand) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.interactEntity(entityId, hand);
    }

    public void swingHand(String name, int hand) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.swingHand(hand);
    }

    public void setHeldItemSlot(String name, int slot) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.setHeldItemSlot(slot);
    }

    public int botCount() { return bots.size(); }

    // ── Inventory delegations ───────────────────────────────────────────────

    /**
     * @return a snapshot of the container with the given id, or {@code null}
     *         if the bot has not received any content for it (other than the
     *         always-present player inventory at containerId 0)
     */
    public BotClient.ContainerSnapshot queryInventory(String name, int containerId) {
        BotClient client = bots.get(name);
        if (client == null) return null;
        return client.snapshotInventory(containerId);
    }

    public void clickContainer(String name, int containerId, int slot, int button, String clickTypeName) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.clickContainer(containerId, slot, button, clickTypeName);
    }

    public void closeContainer(String name, int containerId) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.closeContainer(containerId);
    }

    public void dropHeldItem(String name, boolean dropAll) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.dropHeldItem(dropAll);
    }

    public void setCreativeSlot(String name, int slot, int itemId, int amount) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.setCreativeSlot(slot, itemId, amount);
    }

    // ── Tab-completion, entity snapshots, self-state (B+C) ─────────────────

    public java.util.List<BotClient.EntitySnapshot> entitySnapshots(String name) {
        BotClient client = bots.get(name);
        if (client == null) return java.util.Collections.emptyList();
        return client.entitySnapshots();
    }

    public BotClient.EntitySnapshot entitySnapshot(String name, int entityId) {
        BotClient client = bots.get(name);
        if (client == null) return null;
        return client.entitySnapshot(entityId);
    }

    public BotClient.AbilitiesSnapshot abilities(String name) {
        BotClient client = bots.get(name);
        if (client == null) return null;
        return client.abilities();
    }

    public java.util.List<BotClient.BlockSnapshot> blockSnapshots(String name) {
        BotClient client = bots.get(name);
        if (client == null) return java.util.Collections.emptyList();
        return client.blockSnapshots();
    }

    public void clearBlockSnapshots(String name) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.clearBlockSnapshots();
    }

    public BotClient.ExplosionSnapshot latestExplosion(String botName) {
        BotClient client = bots.get(botName);
        if (client == null) return null;
        return client.latestExplosion();
    }

    /**
     * Issues a tab-completion request and blocks up to {@code timeoutMs} for the
     * server's reply. Returns the suggestion list (possibly empty) or throws
     * on timeout/error.
     */
    public java.util.List<BotClient.Suggestion> queryTabComplete(
            String name, String text, long timeoutMs) throws Exception {
        BotClient client = bots.get(name);
        if (client == null) throw new IllegalStateException("Bot not found: " + name);
        return client.queryTabComplete(text).get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public void respawn(String name) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.respawn();
    }

    public void sendPlayerInput(String name,
                                boolean forward, boolean backward,
                                boolean left, boolean right,
                                boolean jump, boolean sneak, boolean sprint) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.sendPlayerInput(forward, backward, left, right, jump, sneak, sprint);
    }

    public void setFlying(String name, boolean enable) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.setFlying(enable);
    }

    public void useItem(String name, int hand) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.useItem(hand);
    }

    // ── Event-listener wiring for the TcpServer ─────────────────────────────

    public void setChatEventListener(BiConsumer<String, String> listener) { this.chatEventListener = listener; }
    public void setDisconnectListener(BiConsumer<String, String> listener) { this.disconnectListener = listener; }
    public void setInventoryListener(BiConsumer<String, Integer> listener) { this.inventoryListener = listener; }
    public void setServerJoinListener(BiConsumer<String, String> listener) { this.serverJoinListener = listener; }
    public void setEntityListener(BiConsumer<String, Object> listener) { this.entityListener = listener; }
    public void setAbilitiesListener(BiConsumer<String, BotClient.AbilitiesSnapshot> listener) { this.abilitiesListener = listener; }
    public void setBlockListener(BiConsumer<String, Object> listener) { this.blockListener = listener; }
    public void setExplosionListener(BiConsumer<String, Object> listener) { this.explosionListener = listener; }
}
