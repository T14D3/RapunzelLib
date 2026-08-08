package de.t14d3.rapunzellib.devrunner.bot;

import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages bot connections for the DevRunner, keyed by {@code (bot name, server
 * name)}.
 *
 * <p>Multiple backend servers (e.g. "lobby" and "survival") run live-test
 * suites concurrently and all talk to the same bot TCP server. Each server may
 * use the same bot names (BotAlice/BotBob), so bots are keyed by name
 * <em>and</em> the logical server they are connected to. A connect request for
 * a (name, server) pair that already has a connected bot is a no-op; a bot
 * living on another server is left untouched.</p>
 *
 * <p>Also exposes a way to push server-side events (chat arrivals, disconnects,
 * server transfers) that the {@link BotTcpServer} surfaces to connected
 * {@code RpcBotService} clients. Events carry the logical server name so each
 * client can filter for the bots it owns.</p>
 */
public class BotManager {

    /** Key = {@code name + "\u0000" + server}. */
    private final Map<String, BotClient> bots = new ConcurrentHashMap<>();

    /** Tri-consumer carrying (botName, serverName, payload). */
    @FunctionalInterface
    public interface BotEventConsumer<T> {
        void accept(String botName, String serverName, T payload);
    }

    /** Listener fired when a bot receives a chat message. (botName, serverName, message) */
    private volatile BotEventConsumer<String> chatEventListener;
    /** Listener fired when a bot disconnects. (botName, serverName, reasonOrNull) */
    private volatile BotEventConsumer<String> disconnectListener;
    /** Listener fired when a bot's inventory updates. (botName, serverName, containerId) */
    private volatile BotEventConsumer<Integer> inventoryListener;
    /** Listener fired when a bot joins the server (post-transfer ready). (botName, serverName, serverHost) */
    private volatile BotEventConsumer<String> serverJoinListener;
    /** Listener fired when a bot's entity tracking updates. (botName, serverName, unused) */
    private volatile BotEventConsumer<Object> entityListener; // Object = unused payload
    /** Listener fired when a bot's abilities update. (botName, serverName, snapshot) */
    private volatile BotEventConsumer<BotClient.AbilitiesSnapshot> abilitiesListener;
    /** Listener fired when a bot's block tracking updates. (botName, serverName, unused) */
    private volatile BotEventConsumer<Object> blockListener;

    /** Listener fired when a bot receives an explosion event. (botName, serverName, unused) */
    private volatile BotEventConsumer<Object> explosionListener;

    private static String key(String name, String server) {
        return name + "\u0000" + (server != null ? server : "");
    }

    /**
     * Resolves the bot for the given name and server. When the server is
     * unknown/blank, falls back to any bot with that name (backward
     * compatibility for requests that predate the server field).
     */
    private BotClient bot(String name, String server) {
        BotClient client = bots.get(key(name, server));
        if (client != null) return client;
        if (server == null || server.isEmpty()) {
            for (Map.Entry<String, BotClient> e : bots.entrySet()) {
                String n = e.getKey();
                int sep = n.indexOf('\u0000');
                if (sep > 0 && n.substring(0, sep).equals(name)) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    public void connectBot(String name, String server, String host, int port) throws Exception {
        BotClient existing = bots.get(key(name, server));
        if (existing != null && existing.isConnected()) {
            System.out.println("[devrunner] Bot '" + name + "' already connected to " + host + ":" + port);
            return;
        }
        if (existing != null) {
            existing.disconnect();
            bots.remove(key(name, server));
        }
        BotClient client = new BotClient(name, host, port);
        wireEventListeners(name, server, client, host);
        client.connect();
        bots.put(key(name, server), client);
        System.out.println("[devrunner] Bot '" + name + "' connected to " + host + ":" + port);
    }

    private void wireEventListeners(String name, String server, BotClient client, String host) {
        client.addChatCallback(message -> {
            BotEventConsumer<String> l = chatEventListener;
            if (l != null) l.accept(name, server, message);
        });
        client.addInventoryListener((containerId, stateId) -> {
            BotEventConsumer<Integer> l = inventoryListener;
            if (l != null) l.accept(name, server, containerId);
        });
        client.addEntityListener(() -> {
            BotEventConsumer<Object> l = entityListener;
            if (l != null) l.accept(name, server, null);
        });
        client.addAbilitiesListener(snap -> {
            BotEventConsumer<BotClient.AbilitiesSnapshot> l = abilitiesListener;
            if (l != null) l.accept(name, server, snap);
        });
        client.addBlockListener(() -> {
            BotEventConsumer<Object> l = blockListener;
            if (l != null) l.accept(name, server, null);
        });
        client.addExplosionListener(() -> {
            BotEventConsumer<Object> l = explosionListener;
            if (l != null) l.accept(name, server, null);
        });
        BotEventConsumer<String> joins = serverJoinListener;
        if (joins != null) {
            // Fire once after a successful connect - the BotClient.connect()
            // has already returned, so we notify on a worker thread.
            new Thread(() -> {
                try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                joins.accept(name, server, host);
            }, "bot-join-notify-" + name).start();
        }
    }

    public void disconnectBot(String name, String server) {
        String k = key(name, server);
        BotClient client = bots.remove(k);
        if (client == null && (server == null || server.isEmpty())) {
            client = bot(name, "");
            if (client != null) {
                for (Map.Entry<String, BotClient> e : bots.entrySet()) {
                    if (e.getValue() == client) {
                        bots.remove(e.getKey());
                        break;
                    }
                }
            }
        }
        if (client != null) {
            client.disconnect();
            BotEventConsumer<String> l = disconnectListener;
            if (l != null) l.accept(name, server != null ? server : "", null);
            System.out.println("[devrunner] Bot '" + name + "' disconnected");
        }
    }

    public void execute(String name, String server, String command) {
        BotClient client = bot(name, server);
        if (client == null) { System.out.println("[devrunner] Cannot execute on bot '" + name + "': not found"); return; }
        client.sendChat(command);
        System.out.println("[devrunner] Bot '" + name + "' executing: " + command);
    }

    public BotClient getBot(String name, String server) { return bot(name, server); }

    public boolean hasBot(String name, String server) { return bot(name, server) != null; }

    public void digBlock(String name, String server, int x, int y, int z, int direction) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.digBlock(x, y, z, direction);
    }

    public void useItemOn(String name, String server, int x, int y, int z, int hand, int direction) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.useItemOn(x, y, z, hand, direction);
    }

    public void disconnectAll() {
        for (Map.Entry<String, BotClient> entry : bots.entrySet()) {
            entry.getValue().disconnect();
        }
        bots.clear();
    }

    public double[] queryPosition(String name, String server) {
        BotClient client = bot(name, server);
        if (client == null) return null;
        return new double[]{client.getX(), client.getY(), client.getZ(), client.getYaw(), client.getPitch()};
    }

    public float[] queryHealth(String name, String server) {
        BotClient client = bot(name, server);
        if (client == null) return null;
        return new float[]{client.getHealth(), client.getFood(), client.getSaturation()};
    }

    public int[] queryHeldItem(String name, String server) {
        BotClient client = bot(name, server);
        if (client == null) return null;
        return new int[]{client.getHeldItemSlot()};
    }

    public String queryGameMode(String name, String server) {
        BotClient client = bot(name, server);
        if (client == null) return "unknown";
        return client.getGameMode();
    }

    public int queryOpenContainerId(String name, String server) {
        BotClient client = bot(name, server);
        if (client == null) return -1;
        return client.getOpenContainerId();
    }

    public int[] findEntities(String name, String server, String typeName) {
        BotClient client = bot(name, server);
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

    public void moveTo(String name, String server, int x, int y, int z) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.moveTo(x, y, z);
    }

    public void attackEntity(String name, String server, int entityId) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.attackEntity(entityId);
    }

    public void interactEntity(String name, String server, int entityId, int hand) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.interactEntity(entityId, hand);
    }

    public void swingHand(String name, String server, int hand) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.swingHand(hand);
    }

    public void setHeldItemSlot(String name, String server, int slot) {
        BotClient client = bot(name, server);
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
    public BotClient.ContainerSnapshot queryInventory(String name, String server, int containerId) {
        BotClient client = bot(name, server);
        if (client == null) return null;
        return client.snapshotInventory(containerId);
    }

    public void clickContainer(String name, String server, int containerId, int slot, int button, String clickTypeName) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.clickContainer(containerId, slot, button, clickTypeName);
    }

    public void closeContainer(String name, String server, int containerId) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.closeContainer(containerId);
    }

    public void dropHeldItem(String name, String server, boolean dropAll) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.dropHeldItem(dropAll);
    }

    public void setCreativeSlot(String name, String server, int slot, int itemId, int amount) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.setCreativeSlot(slot, itemId, amount);
    }

    // ── Tab-completion, entity snapshots, self-state (B+C) ─────────────────

    public java.util.List<BotClient.EntitySnapshot> entitySnapshots(String name, String server) {
        BotClient client = bot(name, server);
        if (client == null) return java.util.Collections.emptyList();
        return client.entitySnapshots();
    }

    public BotClient.EntitySnapshot entitySnapshot(String name, String server, int entityId) {
        BotClient client = bot(name, server);
        if (client == null) return null;
        return client.entitySnapshot(entityId);
    }

    public BotClient.AbilitiesSnapshot abilities(String name, String server) {
        BotClient client = bot(name, server);
        if (client == null) return null;
        return client.abilities();
    }

    public java.util.List<BotClient.BlockSnapshot> blockSnapshots(String name, String server) {
        BotClient client = bot(name, server);
        if (client == null) return java.util.Collections.emptyList();
        return client.blockSnapshots();
    }

    public void clearBlockSnapshots(String name, String server) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.clearBlockSnapshots();
    }

    public BotClient.ExplosionSnapshot latestExplosion(String botName, String server) {
        BotClient client = bot(botName, server);
        if (client == null) return null;
        return client.latestExplosion();
    }

    /**
     * Issues a tab-completion request and blocks up to {@code timeoutMs} for the
     * server's reply. Returns the suggestion list (possibly empty) or throws
     * on timeout/error.
     */
    public java.util.List<BotClient.Suggestion> queryTabComplete(
            String name, String server, String text, long timeoutMs) throws Exception {
        BotClient client = bot(name, server);
        if (client == null) throw new IllegalStateException("Bot not found: " + name);
        return client.queryTabComplete(text).get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public void respawn(String name, String server) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.respawn();
    }

    public void sendPlayerInput(String name, String server,
                                boolean forward, boolean backward,
                                boolean left, boolean right,
                                boolean jump, boolean sneak, boolean sprint) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.sendPlayerInput(forward, backward, left, right, jump, sneak, sprint);
    }

    public void setFlying(String name, String server, boolean enable) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.setFlying(enable);
    }

    public void useItem(String name, String server, int hand) {
        BotClient client = bot(name, server);
        if (client == null) return;
        client.useItem(hand);
    }

    // ── Event-listener wiring for the TcpServer ─────────────────────────────

    public void setChatEventListener(BotEventConsumer<String> listener) { this.chatEventListener = listener; }
    public void setDisconnectListener(BotEventConsumer<String> listener) { this.disconnectListener = listener; }
    public void setInventoryListener(BotEventConsumer<Integer> listener) { this.inventoryListener = listener; }
    public void setServerJoinListener(BotEventConsumer<String> listener) { this.serverJoinListener = listener; }
    public void setEntityListener(BotEventConsumer<Object> listener) { this.entityListener = listener; }
    public void setAbilitiesListener(BotEventConsumer<BotClient.AbilitiesSnapshot> listener) { this.abilitiesListener = listener; }
    public void setBlockListener(BotEventConsumer<Object> listener) { this.blockListener = listener; }
    public void setExplosionListener(BotEventConsumer<Object> listener) { this.explosionListener = listener; }
}
