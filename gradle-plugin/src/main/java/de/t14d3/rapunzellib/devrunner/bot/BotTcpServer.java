package de.t14d3.rapunzellib.devrunner.bot;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * TCP server that receives bot commands from the server-side {@code RpcBotService}
 * and dispatches them to a {@link BotManager}.
 *
 * <p>Protocol: JSON lines over TCP. Each line is a complete JSON object.
 * Requests have an {@code "id"} field for correlation; responses echo it back.
 * The server runs on a configurable port and accepts multiple concurrent
 * connections.</p>
 *
 * <p>Besides request/response, the server also <em>pushes</em> events to every
 * connected client: {@code "chat"} (bot received a chat message),
 * {@code "disconnected"} (bot dropped its connection), {@code "server"}
 * (bot finished arriving on a new server), {@code "inventory"} (bot's
 * inventory state changed), {@code "entity"} (an entity snapshot was
 * added/updated/removed), {@code "abilities"} (player ability flags
 * changed). Clients route these through their event dispatcher.</p>
 */
public class BotTcpServer implements AutoCloseable {

    private static final Gson GSON = new Gson();

    /** Resolves a server name (e.g. "lobby") to a "host:port" address. */
    @FunctionalInterface
    public interface ServerAddressResolver {
        /** @return "host:port" string, or null if unknown */
        String resolve(String serverName);
    }

    /**
     * Routes a freshly connected bot to its target backend. Implementations
     * typically issue the velocity {@code send <bot> <server>} console command.
     * Invoked after the bot's join-game packet arrived (i.e. the player is
     * registered with the proxy), with a small grace delay.
     */
    @FunctionalInterface
    public interface BotServerRouter {
        void route(String botName, String server);
    }

    private final BotManager botManager;
    private final int port;
    private final ServerAddressResolver addressResolver;
    private final BotServerRouter serverRouter;
    private final ExecutorService executor;
    private ServerSocket serverSocket;
    private volatile boolean running;

    /** Currently open client connections (so we can broadcast events). */
    private final List<ClientConn> clients = new CopyOnWriteArrayList<>();

    public BotTcpServer(BotManager botManager, int port, ServerAddressResolver addressResolver,
                        BotServerRouter serverRouter) {
        this.botManager = botManager;
        this.port = port;
        this.addressResolver = addressResolver;
        this.serverRouter = serverRouter;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "bot-tcp-server");
            t.setDaemon(true);
            return t;
        });
        wireEventListeners();
    }

    private void wireEventListeners() {
        botManager.setChatEventListener((botName, server, message) -> {
            System.out.println("[devrunner] EVENT chat bot=" + botName + " server=" + server + " msg=" + safeString(message));
            broadcast(event("chat", botName, server, "message", safeString(message)));
        });
        botManager.setDisconnectListener((botName, server, reason) ->
                broadcast(event("disconnected", botName, server, "message", reason != null ? reason : "")));
        botManager.setServerJoinListener((botName, server, serverHost) ->
                broadcast(event("server", botName, server, "message", serverHost != null ? serverHost : "")));
        botManager.setInventoryListener(this::pushInventoryEvent);
        botManager.setEntityListener((botName, server, ignored) -> pushEntityEvent(botName, server));
        botManager.setAbilitiesListener(this::pushAbilitiesEvent);
        botManager.setBlockListener((botName, server, ignored) -> pushBlockEvent(botName, server));
        botManager.setExplosionListener((botName, server, ignored) -> pushExplosionEvent(botName, server));
    }

    /** Starts the server and returns the actual port it's listening on. */
    public int start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        int actualPort = serverSocket.getLocalPort();
        System.out.println("[devrunner] Bot TCP server listening on port " + actualPort);
        executor.submit(this::acceptLoop);
        return actualPort;
    }

    private void acceptLoop() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                executor.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (running) {
                    System.err.println("[devrunner] Bot TCP server accept error: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket client) {
        String clientAddr = client.getRemoteSocketAddress().toString();
        ClientConn conn = new ClientConn(client);
        clients.add(conn);
        try (client;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {

            conn.writer = writer;
            String line;
            while (running && (line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    JsonObject request = GSON.fromJson(line, JsonObject.class);
                    if (request == null) continue;
                    JsonObject response = handleRequest(request);
                    if (response != null) {
                        writer.println(GSON.toJson(response));
                    }
                } catch (Exception e) {
                    System.err.println("[devrunner] Bot TCP server error processing: " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[devrunner] Bot TCP client disconnected: " + clientAddr + " - " + e.getMessage());
            }
        } finally {
            clients.remove(conn);
        }
    }

    private JsonObject handleRequest(JsonObject request) {
        String type = request.get("type").getAsString();
        String botName = request.has("bot") ? request.get("bot").getAsString() : "";
        // Logical server the request targets; empty for legacy clients (they
        // fall back to any bot with the given name).
        String server = request.has("server") ? request.get("server").getAsString() : "";
        long id = request.has("id") ? request.get("id").getAsLong() : -1;

        try {
            switch (type) {
                case "connect" -> {
                    String address = addressResolver.resolve(server);
                    if (address == null) {
                        return error(id, "Unknown server: " + server);
                    }
                    String[] parts = address.split(":");
                    String host = parts[0];
                    int p = Integer.parseInt(parts[1]);
                    boolean newSession = botManager.connectBot(botName, server, host, p);
                    // Broadcast a "server" event with the actual landing server
                    // ONLY when a new session was established. A no-op connect
                    // (bot already connected) must not re-fire an arrival, and
                    // the BotClient's own join listener already broadcasts the
                    // genuine login events (initial join + proxy switches).
                    if (newSession) {
                        broadcast(event("server", botName, server, "message", server));
                        routeBotToTarget(botName, server);
                    }
                    return ok(id);
                }
                case "disconnect" -> {
                    botManager.disconnectBot(botName, server);
                    return ok(id);
                }
                case "announce_transfer" -> {
                    String target = request.has("target") ? request.get("target").getAsString() : "";
                    botManager.announceTransfer(botName, target);
                    return ok(id);
                }
                case "dig" -> {
                    int x = request.get("x").getAsInt();
                    int y = request.get("y").getAsInt();
                    int z = request.get("z").getAsInt();
                    int dir = request.get("dir").getAsInt();
                    botManager.digBlock(botName, server, x, y, z, dir);
                    return ok(id);
                }
                case "use" -> {
                    int x = request.get("x").getAsInt();
                    int y = request.get("y").getAsInt();
                    int z = request.get("z").getAsInt();
                    int hand = request.get("hand").getAsInt();
                    int dir = request.get("dir").getAsInt();
                    botManager.useItemOn(botName, server, x, y, z, hand, dir);
                    return ok(id);
                }
                case "exec" -> {
                    String cmd = request.get("command").getAsString();
                    botManager.execute(botName, server, cmd);
                    return ok(id);
                }
                case "move_to" -> {
                    int x = request.get("x").getAsInt();
                    int y = request.get("y").getAsInt();
                    int z = request.get("z").getAsInt();
                    botManager.moveTo(botName, server, x, y, z);
                    return ok(id);
                }
                case "attack" -> {
                    int entityId = request.get("entityId").getAsInt();
                    botManager.attackEntity(botName, server, entityId);
                    return ok(id);
                }
                case "interact" -> {
                    int entityId = request.get("entityId").getAsInt();
                    int hand = request.get("hand").getAsInt();
                    botManager.interactEntity(botName, server, entityId, hand);
                    return ok(id);
                }
                case "swing" -> {
                    int hand = request.get("hand").getAsInt();
                    botManager.swingHand(botName, server, hand);
                    return ok(id);
                }
                case "set_slot" -> {
                    int slot = request.get("slot").getAsInt();
                    botManager.setHeldItemSlot(botName, server, slot);
                    return ok(id);
                }
                // ── Inventory read ──
                case "query_inventory" -> {
                    int containerId = request.get("containerId").getAsInt();
                    BotClient.ContainerSnapshot snap = botManager.queryInventory(botName, server, containerId);
                    if (snap == null) return error(id, "Bot not connected or container not tracked");
                    return inventoryResponse(id, snap);
                }
                // ── Inventory write ──
                case "click_slot" -> {
                    int containerId = request.get("containerId").getAsInt();
                    int slot = request.get("slot").getAsInt();
                    int button = request.get("button").getAsInt();
                    String clickType = request.get("clickType").getAsString();
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    botManager.clickContainer(botName, server, containerId, slot, button, clickType);
                    return ok(id);
                }
                case "close_container" -> {
                    int containerId = request.get("containerId").getAsInt();
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    botManager.closeContainer(botName, server, containerId);
                    return ok(id);
                }
                case "drop_item" -> {
                    boolean dropAll = request.get("dropAll").getAsBoolean();
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    botManager.dropHeldItem(botName, server, dropAll);
                    return ok(id);
                }
                case "creative_slot" -> {
                    int slot = request.get("slot").getAsInt();
                    int itemId = request.get("itemId").getAsInt();
                    int amount = request.get("amount").getAsInt();
                    // componentsJson is intentionally ignored - the slim
                    // transport carries only id+amount.
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    botManager.setCreativeSlot(botName, server, slot, itemId, amount);
                    return ok(id);
                }
                // ── Tab-completion (B) ──
                case "tab_complete" -> {
                    String text = request.get("text").getAsString();
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    try {
                        java.util.List<BotClient.Suggestion> suggestions =
                                botManager.queryTabComplete(botName, server, text, 30_000);
                        JsonObject res = new JsonObject();
                        res.addProperty("type", "tab_complete");
                        res.addProperty("id", id);
                        JsonArray arr = new JsonArray();
                        for (BotClient.Suggestion s : suggestions) {
                            JsonObject o = new JsonObject();
                            o.addProperty("match", s.match);
                            if (s.tooltip != null) o.addProperty("tooltip", s.tooltip);
                            arr.add(o);
                        }
                        res.add("suggestions", arr);
                        return res;
                    } catch (Exception ex) {
                        return error(id, ex.getMessage() != null ? ex.getMessage() : ex.toString());
                    }
                }
                // ── Entity snapshots (B/C) ──
                case "query_entities_full" -> {
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "entities_full");
                    res.addProperty("id", id);
                    JsonArray arr = new JsonArray();
                    for (BotClient.EntitySnapshot s : botManager.entitySnapshots(botName, server)) arr.add(entityJson(s));
                    res.add("entities", arr);
                    return res;
                }
                // ── Block snapshots ──
                case "query_blocks" -> {
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "blocks");
                    res.addProperty("id", id);
                    JsonArray arr = new JsonArray();
                    for (BotClient.BlockSnapshot s : botManager.blockSnapshots(botName, server)) arr.add(blockJson(s));
                    res.add("blocks", arr);
                    return res;
                }
                case "clear_blocks" -> {
                    botManager.clearBlockSnapshots(botName, server);
                    return ok(id);
                }
                case "query_explosion" -> {
                    BotClient.ExplosionSnapshot snap = botManager.latestExplosion(botName, server);
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "explosion");
                    res.addProperty("id", id);
                    if (snap != null) {
                        res.addProperty("x", snap.x());
                        res.addProperty("y", snap.y());
                        res.addProperty("z", snap.z());
                        res.addProperty("radius", snap.radius());
                    }
                    return res;
                }
                case "query_entity" -> {
                    int entityId = request.get("entityId").getAsInt();
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    BotClient.EntitySnapshot s = botManager.entitySnapshot(botName, server, entityId);
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "entity");
                    res.addProperty("id", id);
                    if (s != null) {
                        res.add("entities", singleEntityArray(s));
                    } else {
                        res.add("entities", new JsonArray());
                    }
                    return res;
                }
                // ── Player self-state (C) ──
                case "query_abilities" -> {
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    BotClient.AbilitiesSnapshot a = botManager.abilities(botName, server);
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "abilities");
                    res.addProperty("id", id);
                    if (a != null) {
                        res.addProperty("invincible", a.invincible);
                        res.addProperty("canFly", a.canFly);
                        res.addProperty("flying", a.flying);
                        res.addProperty("creative", a.creative);
                        res.addProperty("flySpeed", a.flySpeed);
                        res.addProperty("walkSpeed", a.walkSpeed);
                    }
                    return res;
                }
                case "respawn" -> {
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    botManager.respawn(botName, server);
                    return ok(id);
                }
                case "player_input" -> {
                    boolean forward = bool(request, "forward");
                    boolean backward = bool(request, "backward");
                    boolean left = bool(request, "left");
                    boolean right = bool(request, "right");
                    boolean jump = bool(request, "jump");
                    boolean sneak = bool(request, "sneak");
                    boolean sprint = bool(request, "sprint");
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    botManager.sendPlayerInput(botName, server, forward, backward, left, right, jump, sneak, sprint);
                    return ok(id);
                }
                case "set_flying" -> {
                    boolean flying = request.get("flying").getAsBoolean();
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    botManager.setFlying(botName, server, flying);
                    return ok(id);
                }
                case "use_item" -> {
                    int hand = request.get("hand").getAsInt();
                    if (!botManager.hasBot(botName, server)) return error(id, "Bot not found: " + botName);
                    botManager.useItem(botName, server, hand);
                    return ok(id);
                }
                // ── Existing read-only queries ──
                case "query_position" -> {
                    double[] pos = botManager.queryPosition(botName, server);
                    if (pos == null) return error(id, "Bot not found");
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "position");
                    res.addProperty("id", id);
                    res.addProperty("x", pos[0]);
                    res.addProperty("y", pos[1]);
                    res.addProperty("z", pos[2]);
                    res.addProperty("yaw", pos[3]);
                    res.addProperty("pitch", pos[4]);
                    return res;
                }
                case "query_health" -> {
                    float[] health = botManager.queryHealth(botName, server);
                    if (health == null) return error(id, "Bot not found");
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "health");
                    res.addProperty("id", id);
                    res.addProperty("health", health[0]);
                    res.addProperty("food", health[1]);
                    res.addProperty("saturation", health[2]);
                    return res;
                }
                case "query_held_item" -> {
                    int[] item = botManager.queryHeldItem(botName, server);
                    if (item == null) return error(id, "Bot not found");
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "held_item");
                    res.addProperty("id", id);
                    res.addProperty("slot", item[0]);
                    return res;
                }
                case "query_gamemode" -> {
                    String gm = botManager.queryGameMode(botName, server);
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "gamemode");
                    res.addProperty("id", id);
                    res.addProperty("gamemode", gm);
                    return res;
                }
                case "query_open_container" -> {
                    int containerId = botManager.queryOpenContainerId(botName, server);
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "open_container");
                    res.addProperty("id", id);
                    res.addProperty("containerId", containerId);
                    return res;
                }
                case "query_entities" -> {
                    String typeName = request.get("entityType").getAsString();
                    int[] ids = botManager.findEntities(botName, server, typeName);
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "entities");
                    res.addProperty("id", id);
                    var arr = new JsonArray();
                    for (int eid : ids) arr.add(eid);
                    res.add("entityIds", arr);
                    return res;
                }
                default -> {
                    return error(id, "Unknown request type: " + type);
                }
            }
        } catch (Exception e) {
            return error(id, e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    // ── Event broadcasting ─────────────────────────────────────────────────

    /**
     * Moves a freshly connected bot to its requested backend. Bots connect to
     * the proxy directly (no forced-host hostnames), so they initially land on
     * the proxy's default server; a small grace delay before the {@code send}
     * makes the routing reliable without any /etc/hosts or DNS setup.
     *
     * <p>When the proxy's log observation already shows the bot on the
     * requested backend (e.g. it IS the default server), the {@code send} is
     * skipped entirely - sending to the current server only produces a
     * confusing "already connected to this server" message.</p>
     */
    private void routeBotToTarget(String botName, String server) {
        if (serverRouter == null || server == null || server.isBlank()) return;
        try {
            Thread.sleep(500);
            String landed = botManager.lastLandingServer(botName);
            if (server.equals(landed)) {
                System.out.println("[devrunner] Bot '" + botName + "' already on target server '"
                        + server + "'; skipping send");
                return;
            }
            serverRouter.route(botName, server);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static JsonObject event(String type, String botName, String server, String key, String value) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.addProperty("bot", botName);
        if (server != null && !server.isEmpty()) o.addProperty("server", server);
        if (value != null) o.addProperty(key, value);
        return o;
    }

    private static String safeString(String s) {
        return s == null ? "" : s;
    }

    private void pushInventoryEvent(String botName, String server, int containerId) {
        BotClient.ContainerSnapshot snap = botManager.queryInventory(botName, server, containerId);
        if (snap == null) return;
        JsonObject o = inventoryPayload(botName, snap);
        if (server != null && !server.isEmpty()) o.addProperty("server", server);
        broadcast(o);
    }

    private void pushEntityEvent(String botName, String server) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "entity");
        o.addProperty("bot", botName);
        if (server != null && !server.isEmpty()) o.addProperty("server", server);
        JsonArray arr = new JsonArray();
        for (BotClient.EntitySnapshot s : botManager.entitySnapshots(botName, server)) arr.add(entityJson(s));
        o.add("entities", arr);
        broadcast(o);
    }

    private void pushAbilitiesEvent(String botName, String server, BotClient.AbilitiesSnapshot snap) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "abilities");
        o.addProperty("bot", botName);
        if (server != null && !server.isEmpty()) o.addProperty("server", server);
        if (snap != null) {
            o.addProperty("invincible", snap.invincible);
            o.addProperty("canFly", snap.canFly);
            o.addProperty("flying", snap.flying);
            o.addProperty("creative", snap.creative);
            o.addProperty("flySpeed", snap.flySpeed);
            o.addProperty("walkSpeed", snap.walkSpeed);
        }
        broadcast(o);
    }

    private void pushBlockEvent(String botName, String server) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "block");
        o.addProperty("bot", botName);
        if (server != null && !server.isEmpty()) o.addProperty("server", server);
        JsonArray arr = new JsonArray();
        for (BotClient.BlockSnapshot s : botManager.blockSnapshots(botName, server)) arr.add(blockJson(s));
        o.add("blocks", arr);
        broadcast(o);
    }

    private void pushExplosionEvent(String botName, String server) {
        BotClient.ExplosionSnapshot snap = botManager.latestExplosion(botName, server);
        if (snap == null) return;
        JsonObject o = new JsonObject();
        o.addProperty("type", "explosion");
        o.addProperty("bot", botName);
        if (server != null && !server.isEmpty()) o.addProperty("server", server);
        o.addProperty("x", snap.x());
        o.addProperty("y", snap.y());
        o.addProperty("z", snap.z());
        o.addProperty("radius", snap.radius());
        broadcast(o);
    }

    private static JsonObject blockJson(BotClient.BlockSnapshot s) {
        JsonObject o = new JsonObject();
        o.addProperty("x", s.x());
        o.addProperty("y", s.y());
        o.addProperty("z", s.z());
        o.addProperty("stateId", s.blockStateId());
        return o;
    }

    private static JsonObject entityJson(BotClient.EntitySnapshot s) {
        JsonObject o = new JsonObject();
        o.addProperty("entityId", s.entityId);
        o.addProperty("typeId", s.typeId);
        if (s.typeName != null) o.addProperty("typeName", s.typeName);
        o.addProperty("x", s.x);
        o.addProperty("y", s.y);
        o.addProperty("z", s.z);
        o.addProperty("yaw", s.yaw);
        o.addProperty("pitch", s.pitch);
        o.addProperty("headYaw", s.headYaw);
        return o;
    }

    private static JsonArray singleEntityArray(BotClient.EntitySnapshot s) {
        JsonArray arr = new JsonArray();
        arr.add(entityJson(s));
        return arr;
    }

    private static boolean bool(JsonObject request, String key) {
        return request.has(key) && request.get(key).getAsBoolean();
    }

    private static JsonObject inventoryResponse(long id, BotClient.ContainerSnapshot snap) {
        JsonObject res = inventoryPayload("", snap);
        res.addProperty("id", id);
        return res;
    }

    private static JsonObject inventoryPayload(String botName, BotClient.ContainerSnapshot snap) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "inventory");
        if (!botName.isEmpty()) o.addProperty("bot", botName);
        o.addProperty("containerId", snap.containerId);
        o.addProperty("stateId", snap.stateId);
        JsonArray slots = new JsonArray();
        if (snap.slots != null) {
            for (org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack s : snap.slots) {
                slots.add(itemToJson(s));
            }
        }
        o.add("slots", slots);
        o.add("cursorItem", itemToJson(snap.cursorItem));
        return o;
    }

    private static com.google.gson.JsonElement itemToJson(
            org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack s) {
        if (s == null || s.getId() < 0 || s.getAmount() <= 0) return JsonNull.INSTANCE;
        JsonObject o = new JsonObject();
        o.addProperty("id", s.getId());
        o.addProperty("amount", s.getAmount());
        // dataComponentsPatch is intentionally not serialised here - the
        // slim wire representation in livetest leaves componentsJson as null
        // unless explicitly set. Tests that need component data should use
        // a different verification channel.
        return o;
    }

    private void broadcast(JsonObject msg) {
        String json = GSON.toJson(msg);
        for (ClientConn c : clients) {
            try {
                PrintWriter w = c.writer;
                if (w != null) w.println(json);
            } catch (Exception ignored) { /* client most likely dropped */ }
        }
    }

    private static JsonObject ok(long id) {
        JsonObject res = new JsonObject();
        res.addProperty("type", "ok");
        res.addProperty("id", id);
        return res;
    }

    private static JsonObject error(long id, String message) {
        JsonObject res = new JsonObject();
        res.addProperty("type", "error");
        res.addProperty("id", id);
        res.addProperty("message", message);
        return res;
    }

    /** Tracks the writer for each connected client so events can be broadcast. */
    private static final class ClientConn {
        final Socket socket;
        volatile PrintWriter writer;

        ClientConn(Socket socket) { this.socket = socket; }
    }

    @Override
    public void close() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }
}
