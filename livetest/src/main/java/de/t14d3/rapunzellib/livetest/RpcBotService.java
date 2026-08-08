package de.t14d3.rapunzellib.livetest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A {@link BotService} implementation that communicates with the DevRunner's
 * {@code BotTcpServer} over a TCP socket using a JSON-line protocol.
 *
 * <p>Each instance opens one persistent TCP connection to the DevRunner and
 * multiplexes requests for multiple bots over that connection. Responses are
 * correlated by request ID. Server-pushed events (chat, ready, disconnected,
 * inventory snapshots, server transfers, position, etc.) are dispatched to
 * registered {@link BotEventListener listeners} and to one-shot awaiters
 * registered through {@link EventDispatcher}.</p>
 *
 * <p>The TCP reader runs on a single dedicated thread; the dispatcher's
 * await primitives avoid the per-poll Thread.sleep busy-loop that the older
 * implementation used.</p>
 */
public class RpcBotService implements BotService, AutoCloseable {

    private static final Gson GSON = new Gson();
    private static final String PORT_PROPERTY = "rapunzellib.bot.rpc.port";
    private static final int DEFAULT_PORT = 26566;

    private volatile Socket socket;
    private final String host;
    private final int port;
    private volatile PrintWriter writer;
    private volatile BufferedReader reader;
    private final Object writeLock = new Object();
    private final AtomicLong requestIdCounter = new AtomicLong(1);
    private final Map<Long, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    private final EventDispatcher dispatcher = new EventDispatcher();
    private final ScheduledExecutorService timeoutScheduler = newTimeoutScheduler();

    // Local cache of the latest inventory snapshot per (bot,containerId). This
    // lets awaitInventory check the present state without waiting for an event.
    private final Map<String, Map<Integer, BotInventory>> inventoryCache = new ConcurrentHashMap<>();

    // Local cache of the latest entity snapshot per (bot, entityId). This lets
    // awaitEntity/queryMatchingEntities consult the present state without
    // waiting for an event.
    private final Map<String, Map<Integer, BotEntity>> entityCache = new ConcurrentHashMap<>();

    // Local cache of block-change snapshots per bot. Uses ConcurrentLinkedDeque
    // for FIFO semantics - newest entries at the tail.
    private final Map<String, java.util.concurrent.ConcurrentLinkedDeque<BlockSnapshot>> blockCache = new ConcurrentHashMap<>();

    // Local cache of the latest explosion snapshot per bot.
    private final Map<String, ExplosionSnapshot> explosionCache = new ConcurrentHashMap<>();

    // Server-arrival cache: botName -> serverName. Populated on "server" events
    // so that awaitServer() can match pre-existing arrivals even when the await
    // slot is registered after the event has already been dispatched.
    private final Map<String, String> serverCache = new ConcurrentHashMap<>();

    // Bots owned by this client: botName -> set of logical servers the bot has
    // been connected to. Used to scope outgoing requests and to filter incoming
    // events (multiple backend servers share one bot TCP server and may use the
    // same bot names concurrently).
    private final Map<String, java.util.Set<String>> botServers = new ConcurrentHashMap<>();
    // Last server each bot was connected to; requests for that bot are routed
    // to that server.
    private final Map<String, String> botPrimaryServer = new ConcurrentHashMap<>();

    // Local cache of the latest abilities snapshot per bot. Populated on
    // "abilities" events so that queryAbilities() can return immediately
    // without waiting for an RPC round-trip - and as a fallback in case the
    // RPC response type collides with the event type (see isEvent + id check).
    private final Map<String, BotAbilities> abilitiesCache = new ConcurrentHashMap<>();

    private volatile boolean running = true;

    public RpcBotService() {
        this("127.0.0.1", Integer.getInteger(PORT_PROPERTY, DEFAULT_PORT));
    }

    public RpcBotService(String host, int port) {
        this.host = host;
        this.port = port;
        this.socket = connectSocket(host, port);
        try {
            this.writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            this.reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open BotRPC transport to " + host + ":" + port, e);
        }
        startReaderThread(this.reader);
    }

    private static Socket connectSocket(String host, int port) {
        try {
            Socket fresh = new Socket();
            fresh.connect(new InetSocketAddress(host, port), 5_000);
            fresh.setSoTimeout(0); // we have our own timeouts via the dispatcher
            return fresh;
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to BotRPC server at " + host + ":" + port, e);
        }
    }

    private void startReaderThread(BufferedReader reader) {
        Thread readerThread = new Thread(this::readLoop, "rpc-bot-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    // ── Transport ───────────────────────────────────────────────────────────

    private CompletableFuture<JsonObject> sendRequest(JsonObject request) {
        long id = requestIdCounter.getAndIncrement();
        request.addProperty("id", id);
        // Route the request to the server this client connected the bot to.
        // The BotTcpServer pools bots per (name, server); without the server
        // field the request could hit a same-named bot on another server.
        if (request.has("bot") && !request.has("server")) {
            String bot = request.get("bot").getAsString();
            String srv = botPrimaryServer.get(bot);
            if (srv != null && !srv.isEmpty()) {
                request.addProperty("server", srv);
            }
        }
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pending.put(id, future);
        // Time out pending requests even if the server never replies.
        timeoutScheduler.schedule(() -> {
            CompletableFuture<JsonObject> removed = pending.remove(id);
            if (removed != null) {
                removed.completeExceptionally(new java.util.concurrent.TimeoutException(
                        "BotRPC request " + request.get("type").getAsString() + " timed out"));
            }
        }, 30, TimeUnit.SECONDS);
        if (!writeLine(request)) {
            // The transport broke (PrintWriter swallows IOExceptions, so a dead
            // socket would otherwise silently drop every request). Try once to
            // restore it; if that fails, fail loudly instead of hanging.
            try {
                reconnectTransport();
                if (writeLine(request)) {
                    return future;
                }
            } catch (IOException e) {
                // fall through to the loud failure below
            }
            pending.remove(id);
            serverCache.clear();
            future.completeExceptionally(new RuntimeException(
                    "BotRPC connection lost; request '" + request.get("type").getAsString()
                            + "' for bot '" + request.get("bot").getAsString() + "' was not delivered"));
        }
        return future;
    }

    /**
     * Writes a request line to the TCP transport. Returns {@code false} when
     * the underlying socket is broken (the PrintWriter error flag is set).
     */
    private boolean writeLine(JsonObject request) {
        synchronized (writeLock) {
            PrintWriter w = writer;
            if (w == null || w.checkError()) return false;
            try {
                w.println(GSON.toJson(request));
            } catch (RuntimeException e) {
                return false;
            }
            return !w.checkError();
        }
    }

    /**
     * Replaces a broken TCP transport with a fresh connection. The old socket
     * is abandoned without closing it so the old reader thread keeps idling
     * instead of failing the shared pending map; a new reader handles the new
     * connection. Stale caches are cleared so {@code awaitServer()} cannot
     * false-positive on arrivals from the dead connection.
     */
    private synchronized void reconnectTransport() throws IOException {
        PrintWriter w = writer;
        if (w == null || !w.checkError()) {
            return; // already healthy (or another thread reconnected first)
        }
        Socket fresh = connectSocket(host, port);
        PrintWriter freshWriter;
        BufferedReader freshReader;
        try {
            freshWriter = new PrintWriter(
                    new OutputStreamWriter(fresh.getOutputStream(), StandardCharsets.UTF_8), true);
            freshReader = new BufferedReader(
                    new InputStreamReader(fresh.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            try {
                fresh.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
        // Publish the new transport *before* starting the reader so the old
        // reader's finally block can no longer fail the pending requests.
        this.socket = fresh;
        this.writer = freshWriter;
        this.reader = freshReader;
        serverCache.clear();
        System.err.println("[rpc-bot] transport reconnected to " + host + ":" + port);
        startReaderThread(freshReader);
    }

    private void sendAndForget(JsonObject request) {
        sendRequest(request); // ignore returned future
    }

    @SuppressWarnings("unused")
    private void fireAndForgetWithoutId(JsonObject request) {
        synchronized (this) {
            writer.println(GSON.toJson(request));
        }
    }

    private void readLoop() {
        BufferedReader myReader = this.reader;
        try {
            String line;
            while (running && (line = myReader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    JsonObject msg = GSON.fromJson(line, JsonObject.class);
                    if (msg == null) continue;
                    handleMessage(msg);
                } catch (Exception e) {
                    // Bad frame - log to stderr but keep the loop alive.
                    if (running) System.err.println("[rpc-bot] parse error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                // Only the current connection may fail the awaits - an
                // abandoned connection replaced by a reconnect must not.
                if (this.reader == myReader) {
                    dispatcher.failAll(new RuntimeException("BotRPC connection lost", e));
                }
            }
        } finally {
            if (this.reader == myReader) {
                for (CompletableFuture<JsonObject> f : pending.values()) {
                    f.completeExceptionally(new RuntimeException("BotRPC connection closed"));
                }
                pending.clear();
            }
        }
    }

    private void handleMessage(JsonObject msg) {
        String type = msg.get("type").getAsString();

        // Server-pushed events: "ready", "chat", "disconnected", "server", "inventory".
        // Events carry no request id; RPC responses echo theirs. Without this disambiguator,
        // response-type overlaps (e.g. "abilities", "inventory") would be routed to event
        // dispatch and leave the caller hanging.
        if (isEvent(type) && !msg.has("id")) {
            String botName = msg.has("bot") ? msg.get("bot").getAsString() : "";
            // Only handle events for bots this client connected. The bot TCP
            // server is shared by multiple backend servers which may use the
            // same bot names concurrently, so events also carry the logical
            // server and must match one this bot was connected to.
            java.util.Set<String> knownServers = botServers.get(botName);
            if (knownServers == null) {
                return;
            }
            String evtServer = msg.has("server") ? msg.get("server").getAsString() : "";
            if (!evtServer.isEmpty() && !knownServers.contains(evtServer)) {
                return;
            }
            String message = msg.has("message") ? msg.get("message").getAsString() : "";
            // Tag the event with its source server so await predicates can be
            // scoped to (bot, server): the devrunner broadcasts every event to
            // every backend, and cross-server tests connect the same bot name
            // to several servers at once.
            BotEventListener.BotEvent event =
                    new BotEventListener.BotEvent(type.toUpperCase(), botName, message, evtServer);
            if ("INVENTORY".equalsIgnoreCase(type)) {
                // Cache the latest inventory so awaitInventory can match immediately.
                BotInventory snap = parseInventoryPayload(msg);
                if (snap != null) {
                    inventoryCache.computeIfAbsent(botName, n -> new ConcurrentHashMap<>())
                            .put(snap.containerId(), snap);
                }
            }
            if ("ENTITY".equalsIgnoreCase(type)) {
                // Server may send either a single entity payload (rare) or an
                // array of snapshots under "entities". Parse all of them and
                // update the cache before dispatch so awaiters see fresh state.
                Map<Integer, BotEntity> byId = entityCache.computeIfAbsent(botName, n -> new ConcurrentHashMap<>());
                if (msg.has("entities") && msg.get("entities").isJsonArray()) {
                    for (JsonElement e : msg.getAsJsonArray("entities")) {
                        BotEntity ent = parseEntityPayload(e.getAsJsonObject());
                        if (ent != null && !ent.isUnknown()) byId.put(ent.entityId(), ent);
                    }
                } else {
                    BotEntity ent = parseEntityPayload(msg);
                    if (ent != null && !ent.isUnknown()) byId.put(ent.entityId(), ent);
                }
            }
            if ("BLOCK".equalsIgnoreCase(type)) {
                java.util.concurrent.ConcurrentLinkedDeque<BlockSnapshot> deque =
                        blockCache.computeIfAbsent(botName, n -> new java.util.concurrent.ConcurrentLinkedDeque<>());
                if (msg.has("blocks") && msg.get("blocks").isJsonArray()) {
                    for (JsonElement e : msg.getAsJsonArray("blocks")) {
                        JsonObject bo = e.getAsJsonObject();
                        int x = bo.get("x").getAsInt();
                        int y = bo.get("y").getAsInt();
                        int z = bo.get("z").getAsInt();
                        int stateId = bo.get("stateId").getAsInt();
                        deque.addLast(new BlockSnapshot(x, y, z, stateId));
                    }
                }
            }
            if ("EXPLOSION".equalsIgnoreCase(type)) {
                ExplosionSnapshot snap = new ExplosionSnapshot(
                        msg.get("x").getAsDouble(),
                        msg.get("y").getAsDouble(),
                        msg.get("z").getAsDouble(),
                        msg.get("radius").getAsFloat());
                explosionCache.put(botName, snap);
            }
            if ("SERVER".equalsIgnoreCase(type)) {
                // Cache the server arrival so awaitServer() can match
                // pre-existing arrivals even when the await slot is registered
                // after the event has been dispatched.
                if (message != null && !message.isEmpty()) {
                    serverCache.put(botName, message);
                }
            }
            if ("ABILITIES".equalsIgnoreCase(type)) {
                // Cache the latest abilities snapshot so queryAbilities()
                // can return it immediately from cache.
                BotAbilities a = parseAbilitiesPayload(msg);
                if (a != null) {
                    abilitiesCache.put(botName, a);
                }
            }
            if ("DISCONNECTED".equalsIgnoreCase(type)) {
                // Clean up caches when a bot disconnects.
                serverCache.remove(botName);
                abilitiesCache.remove(botName);
            }
            // Cache population must happen *before* dispatch so that await
            // predicates (which read the cache) see the new state synchronously.
            dispatcher.dispatch(event);
            return;
        }

        if ("ok".equals(type)) {
            completePending(msg);
            return;
        }
        if ("error".equals(type)) {
            JsonObject finalMsg = msg;
            long id = msg.get("id").getAsLong();
            CompletableFuture<JsonObject> future = pending.remove(id);
            if (future != null) {
                String errMsg = msg.has("message") ? msg.get("message").getAsString() : "Unknown error";
                future.completeExceptionally(new BotRpcException(errMsg, type));
            }
            return;
        }
        // Response carrying a payload - treat as ok-ish and unwrap.
        completePending(msg);
    }

    private void completePending(JsonObject msg) {
        long id = msg.get("id").getAsLong();
        CompletableFuture<JsonObject> future = pending.remove(id);
        if (future != null) future.complete(msg);
    }

    private static boolean isEvent(String type) {
        return "chat".equals(type) || "ready".equals(type)
                || "disconnected".equals(type) || "server".equals(type)
                || "inventory".equals(type) || "entity".equals(type)
                || "abilities".equals(type) || "block".equals(type)
                || "explosion".equals(type);
    }

    // ── BotService implementation ────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<Bot> connect(@NotNull String name, @NotNull String serverName) {
        botServers.computeIfAbsent(name, n -> java.util.concurrent.ConcurrentHashMap.newKeySet())
                .add(serverName);
        botPrimaryServer.put(name, serverName);
        JsonObject req = new JsonObject();
        req.addProperty("type", "connect");
        req.addProperty("bot", name);
        req.addProperty("server", serverName);

        return sendRequest(req).thenApply(resp -> new Bot(name, serverName, this));
    }

    @Override
    public void disconnect(@NotNull String name) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "disconnect");
        req.addProperty("bot", name);
        sendAndForget(req);
    }

    @Override
    public @NotNull CompletableFuture<Void> execute(@NotNull String name, @NotNull String command) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "exec");
        req.addProperty("bot", name);
        req.addProperty("command", command);
        return sendRequest(req).thenAccept(resp -> { /* void */ });
    }

    @Override
    public void digBlock(@NotNull String name, int x, int y, int z, @NotNull Bot.Direction direction) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "dig");
        req.addProperty("bot", name);
        req.addProperty("x", x);
        req.addProperty("y", y);
        req.addProperty("z", z);
        req.addProperty("dir", direction.ordinal());
        sendAndForget(req);
    }

    @Override
    public void useItemOn(@NotNull String name, int x, int y, int z, @NotNull Bot.Hand hand, @NotNull Bot.Direction direction) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "use");
        req.addProperty("bot", name);
        req.addProperty("x", x);
        req.addProperty("y", y);
        req.addProperty("z", z);
        req.addProperty("hand", hand.ordinal());
        req.addProperty("dir", direction.ordinal());
        sendAndForget(req);
    }

    @Override
    public void moveTo(@NotNull String name, int x, int y, int z) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "move_to");
        req.addProperty("bot", name);
        req.addProperty("x", x);
        req.addProperty("y", y);
        req.addProperty("z", z);
        sendAndForget(req);
    }

    @Override
    public void attackEntity(@NotNull String name, int entityId) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "attack");
        req.addProperty("bot", name);
        req.addProperty("entityId", entityId);
        sendAndForget(req);
    }

    @Override
    public void interactEntity(@NotNull String name, int entityId, int hand) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "interact");
        req.addProperty("bot", name);
        req.addProperty("entityId", entityId);
        req.addProperty("hand", hand);
        sendAndForget(req);
    }

    @Override
    public void swingHand(@NotNull String name, int hand) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "swing");
        req.addProperty("bot", name);
        req.addProperty("hand", hand);
        sendAndForget(req);
    }

    @Override
    public void setHeldItemSlot(@NotNull String name, int slot) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "set_slot");
        req.addProperty("bot", name);
        req.addProperty("slot", slot);
        sendAndForget(req);
    }

    // ── Async queries ───────────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<Bot.Position> queryPosition(@NotNull String name, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_position");
        req.addProperty("bot", name);
        return sendRequest(req).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(resp -> new Bot.Position(
                        resp.get("x").getAsDouble(),
                        resp.get("y").getAsDouble(),
                        resp.get("z").getAsDouble(),
                        resp.get("yaw").getAsFloat(),
                        resp.get("pitch").getAsFloat()));
    }

    @Override
    public @NotNull CompletableFuture<Bot.Health> queryHealth(@NotNull String name, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_health");
        req.addProperty("bot", name);
        return sendRequest(req).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(resp -> new Bot.Health(
                        resp.get("health").getAsFloat(),
                        resp.get("food").getAsInt(),
                        resp.get("saturation").getAsFloat()));
    }

    @Override
    public @NotNull CompletableFuture<Bot.HeldItem> queryHeldItem(@NotNull String name, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_held_item");
        req.addProperty("bot", name);
        return sendRequest(req).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(resp -> new Bot.HeldItem(
                        resp.get("slot").getAsInt(),
                        resp.has("material") ? resp.get("material").getAsString() : "unknown"));
    }

    @Override
    public @NotNull CompletableFuture<String> queryGameMode(@NotNull String name, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_gamemode");
        req.addProperty("bot", name);
        return sendRequest(req).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(resp -> resp.get("gamemode").getAsString());
    }

    @Override
    public @NotNull CompletableFuture<Integer> queryOpenContainer(@NotNull String name, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_open_container");
        req.addProperty("bot", name);
        return sendRequest(req).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(resp -> resp.get("containerId").getAsInt());
    }

    @Override
    public @NotNull CompletableFuture<int[]> queryEntities(@NotNull String name, @NotNull String typeName, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_entities");
        req.addProperty("bot", name);
        req.addProperty("entityType", typeName);
        return sendRequest(req).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(resp -> {
                    JsonArray arr = resp.getAsJsonArray("entityIds");
                    int[] result = new int[arr.size()];
                    for (int i = 0; i < arr.size(); i++) result[i] = arr.get(i).getAsInt();
                    return result;
                });
    }

    // ── Inventory read ───────────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<BotInventory> queryInventory(@NotNull String name, int containerId, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_inventory");
        req.addProperty("bot", name);
        req.addProperty("containerId", containerId);
        return sendRequest(req).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(this::parseInventoryPayload);
    }

    @Override
    public @NotNull CompletableFuture<BotInventory> awaitInventory(@NotNull String name,
                                                                    int containerId,
                                                                    @NotNull InventoryMatcher matcher,
                                                                    @NotNull Duration timeout) {
        // 1) Check the local cache first - let pre-existing snapshots short-circuit.
        BotInventory cached = getCachedInventory(name, containerId);
        if (cached != null && matcher.test(cached)) {
            return CompletableFuture.completedFuture(cached);
        }

        // 2) Otherwise register an await predicate that fires on subsequent
        //    INVENTORY pushes for this (bot,containerId) pair, AND also polls
        //    the cache (in case a push landed before we registered).
        EventDispatcher.AwaitSlot slot = new EventDispatcher.AwaitSlot(event -> {
            if (!"INVENTORY".equals(event.type())) return false;
            if (!name.equals(event.botName())) return false;
            if (!serverMatchesScope(event, botPrimaryServer.get(name))) return false;
            BotInventory snap = getCachedInventory(name, containerId);
            return snap != null && matcher.test(snap);
        });
        dispatcher.registerAwait(slot);

        CompletableFuture<BotEventListener.BotEvent> prepared =
                slot.future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                        .handle((event, ex) -> {
                            dispatcher.removeAwait(slot);
                            if (ex != null) {
                                Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                                throw new RuntimeException("awaitInventory(" + name + ", container=" + containerId
                                        + ") timed out or failed: " + cause.getMessage(), cause);
                            }
                            return event;
                        });
        return prepared.thenApply(event -> {
            BotInventory snap = getCachedInventory(name, containerId);
            if (snap == null) {
                throw new RuntimeException("awaitInventory(" + name + ", container=" + containerId
                        + ") completed without a snapshot in cache");
            }
            return snap;
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> clickSlot(@NotNull String name,
                                                      int containerId,
                                                      int slot,
                                                      int button,
                                                      @NotNull BotInventory.ClickType clickType) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "click_slot");
        req.addProperty("bot", name);
        req.addProperty("containerId", containerId);
        req.addProperty("slot", slot);
        req.addProperty("button", button);
        req.addProperty("clickType", clickType.name());
        return sendRequest(req).thenAccept(resp -> { /* void */ });
    }

    @Override
    public @NotNull CompletableFuture<Void> closeContainer(@NotNull String name, int containerId) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "close_container");
        req.addProperty("bot", name);
        req.addProperty("containerId", containerId);
        return sendRequest(req).thenAccept(resp -> { /* void */ });
    }

    @Override
    public @NotNull CompletableFuture<Void> dropHeldItem(@NotNull String name, boolean dropAll) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "drop_item");
        req.addProperty("bot", name);
        req.addProperty("dropAll", dropAll);
        return sendRequest(req).thenAccept(resp -> { /* void */ });
    }

    @Override
    public @NotNull CompletableFuture<Void> setCreativeSlot(@NotNull String name, int slot, @NotNull BotItemStack stack) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "creative_slot");
        req.addProperty("bot", name);
        req.addProperty("slot", slot);
        req.addProperty("itemId", stack.id());
        req.addProperty("amount", stack.amount());
        if (stack.componentsJson() != null) req.addProperty("componentsJson", stack.componentsJson());
        return sendRequest(req).thenAccept(resp -> { /* void */ });
    }

    // ── Tab-completion (B) ─────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<java.util.List<BotSuggestion>> queryTabComplete(@NotNull String name, @NotNull String text, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "tab_complete");
        req.addProperty("bot", name);
        req.addProperty("text", text);
        return sendRequest(req).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(resp -> {
                    java.util.List<BotSuggestion> out = new java.util.ArrayList<>();
                    if (resp.has("suggestions") && resp.get("suggestions").isJsonArray()) {
                        for (JsonElement e : resp.getAsJsonArray("suggestions")) {
                            JsonObject o = e.getAsJsonObject();
                            String match = o.get("match").getAsString();
                            String tooltip = o.has("tooltip") && !o.get("tooltip").isJsonNull()
                                    ? o.get("tooltip").getAsString() : null;
                            out.add(new BotSuggestion(match, tooltip));
                        }
                    }
                    return java.util.Collections.unmodifiableList(out);
                });
    }

    // ── Entity snapshots (B/C) ─────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<java.util.List<BotEntity>> queryMatchingEntities(
            @NotNull String name, @NotNull EntityMatcher matcher, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_entities_full");
        req.addProperty("bot", name);
        return sendRequest(req).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(resp -> parseEntityList(resp, matcher));
    }

    @Override
    public @NotNull CompletableFuture<BotEntity> queryEntity(@NotNull String name, int entityId, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_entity");
        req.addProperty("bot", name);
        req.addProperty("entityId", entityId);
        return sendRequest(req).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(resp -> parseEntity(resp, entityId));
    }

    @Override
    public @NotNull CompletableFuture<BotEntity> awaitEntity(
            @NotNull String name, @NotNull EntityMatcher matcher, @NotNull Duration timeout) {
        // 1) Check the local cache first - let pre-existing snapshots short-circuit.
        BotEntity cached = findCachedEntity(name, matcher);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        // 2) Otherwise subscribe to subsequent ENTITY updates.
        EventDispatcher.AwaitSlot slot =
                new EventDispatcher.AwaitSlot(event -> {
                    if (!"ENTITY".equals(event.type())) return false;
                    if (!name.equals(event.botName())) return false;
                    if (!serverMatchesScope(event, botPrimaryServer.get(name))) return false;
                    BotEntity found = findCachedEntity(name, matcher);
                    return found != null;
                });
        dispatcher.registerAwait(slot);
        return slot.future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .handle((event, ex) -> {
                    dispatcher.removeAwait(slot);
                    if (ex != null) {
                        Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                        throw new RuntimeException("awaitEntity(" + name + ") timed out or failed: "
                                + cause.getMessage(), cause);
                    }
                    BotEntity found = findCachedEntity(name, matcher);
                    if (found == null) {
                        throw new RuntimeException("awaitEntity(" + name + ") completed without a matching entity in cache");
                    }
                    return found;
                });
    }

    // ── Block-change snapshots ──────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<Void> awaitBlock(@NotNull String botName,
                                                       int x, int y, int z,
                                                       int expectedStateId,
                                                       long timeoutMs) {
        // 1) Check the local cache first - allow pre-existing snapshots to
        //    short-circuit.
        if (hasCachedBlock(botName, x, y, z, expectedStateId)) {
            return CompletableFuture.completedFuture(null);
        }

        // 2) Otherwise subscribe to subsequent BLOCK events.
        EventDispatcher.AwaitSlot slot = new EventDispatcher.AwaitSlot(event -> {
            if (!"BLOCK".equals(event.type())) return false;
            if (!botName.equals(event.botName())) return false;
            if (!serverMatchesScope(event, botPrimaryServer.get(botName))) return false;
            return hasCachedBlock(botName, x, y, z, expectedStateId);
        });
        dispatcher.registerAwait(slot);
        return slot.future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .handle((event, ex) -> {
                    dispatcher.removeAwait(slot);
                    if (ex != null) {
                        Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                        throw new RuntimeException("awaitBlock(" + botName + ", " + x + "," + y + "," + z
                                + ") timed out or failed: " + cause.getMessage(), cause);
                    }
                    if (!hasCachedBlock(botName, x, y, z, expectedStateId)) {
                        throw new RuntimeException("awaitBlock(" + botName + ") completed without matching block in cache");
                    }
                    return null;
                });
    }

    @Override
    public @NotNull CompletableFuture<Void> awaitBlock(int x, int y, int z,
                                                       int expectedStateId,
                                                       long timeoutMs) {
        // Use the first bot name known to this service.
        String firstBot = blockCache.keySet().stream().findFirst().orElse(null);
        if (firstBot == null) {
            // Fall back to waiting for any bot - the event predicate will match
            // the first BLOCK event from any bot whose cache contains the block.
            EventDispatcher.AwaitSlot slot = new EventDispatcher.AwaitSlot(event -> {
                if (!"BLOCK".equals(event.type())) return false;
                return hasCachedBlock(event.botName(), x, y, z, expectedStateId);
            });
            dispatcher.registerAwait(slot);
            return slot.future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .handle((event, ex) -> {
                        dispatcher.removeAwait(slot);
                        if (ex != null) {
                            Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                            throw new RuntimeException("awaitBlock(any, " + x + "," + y + "," + z
                                    + ") timed out or failed: " + cause.getMessage(), cause);
                        }
                        return null;
                    });
        }
        return awaitBlock(firstBot, x, y, z, expectedStateId, timeoutMs);
    }

    @Override
    public @NotNull CompletableFuture<java.util.List<BlockSnapshot>> queryBlocks(@NotNull String botName) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_blocks");
        req.addProperty("bot", botName);
        return sendRequest(req).orTimeout(30_000, TimeUnit.MILLISECONDS)
                .thenApply(resp -> parseBlocksList(resp));
    }

    @Override
    public void clearBlocks(@NotNull String botName) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "clear_blocks");
        req.addProperty("bot", botName);
        sendAndForget(req);
        // Also clear the local cache.
        java.util.concurrent.ConcurrentLinkedDeque<BlockSnapshot> deque = blockCache.get(botName);
        if (deque != null) deque.clear();
    }

    // ── Explosion tracking ──────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<Void> awaitExplosion(@NotNull String botName, float minRadius, long timeoutMs) {
        // 1) Check the local cache first.
        ExplosionSnapshot cached = explosionCache.get(botName);
        if (cached != null && cached.radius() >= minRadius) {
            return CompletableFuture.completedFuture(null);
        }

        // 2) Otherwise subscribe to subsequent EXPLOSION events.
        EventDispatcher.AwaitSlot slot = new EventDispatcher.AwaitSlot(event -> {
            if (!"EXPLOSION".equals(event.type())) return false;
            if (!botName.equals(event.botName())) return false;
            if (!serverMatchesScope(event, botPrimaryServer.get(botName))) return false;
            ExplosionSnapshot snap = explosionCache.get(botName);
            return snap != null && snap.radius() >= minRadius;
        });
        dispatcher.registerAwait(slot);
        return slot.future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .handle((event, ex) -> {
                    dispatcher.removeAwait(slot);
                    if (ex != null) {
                        Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                        throw new RuntimeException("awaitExplosion(" + botName + ", minRadius=" + minRadius
                                + ") timed out or failed: " + cause.getMessage(), cause);
                    }
                    ExplosionSnapshot snap = explosionCache.get(botName);
                    if (snap == null || snap.radius() < minRadius) {
                        throw new RuntimeException("awaitExplosion(" + botName
                                + ") completed without matching explosion in cache");
                    }
                    return null;
                });
    }

    @Override
    public @NotNull CompletableFuture<Void> awaitExplosion(float minRadius, long timeoutMs) {
        // Use the first bot name known to this service.
        String firstBot = explosionCache.keySet().stream().findFirst().orElse(null);
        if (firstBot == null) {
            // Fall back to waiting for any bot.
            EventDispatcher.AwaitSlot slot = new EventDispatcher.AwaitSlot(event -> {
                if (!"EXPLOSION".equals(event.type())) return false;
                ExplosionSnapshot snap = explosionCache.get(event.botName());
                return snap != null && snap.radius() >= minRadius;
            });
            dispatcher.registerAwait(slot);
            return slot.future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .handle((event, ex) -> {
                        dispatcher.removeAwait(slot);
                        if (ex != null) {
                            Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                            throw new RuntimeException("awaitExplosion(any, minRadius=" + minRadius
                                    + ") timed out or failed: " + cause.getMessage(), cause);
                        }
                        return null;
                    });
        }
        return awaitExplosion(firstBot, minRadius, timeoutMs);
    }

    @Override
    public @NotNull CompletableFuture<ExplosionSnapshot> queryExplosion(@NotNull String botName) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_explosion");
        req.addProperty("bot", botName);
        return sendRequest(req).orTimeout(30_000, TimeUnit.MILLISECONDS)
                .thenApply(resp -> {
                    if (resp.has("x") && resp.has("y") && resp.has("z") && resp.has("radius")) {
                        ExplosionSnapshot snap = new ExplosionSnapshot(
                                resp.get("x").getAsDouble(),
                                resp.get("y").getAsDouble(),
                                resp.get("z").getAsDouble(),
                                resp.get("radius").getAsFloat());
                        explosionCache.put(botName, snap);
                        return snap;
                    }
                    return null;
                });
    }

    private boolean hasCachedBlock(String botName, int x, int y, int z, int expectedStateId) {
        java.util.concurrent.ConcurrentLinkedDeque<BlockSnapshot> deque = blockCache.get(botName);
        if (deque == null) return false;
        // Iterate newest-first (from the tail) for best latency.
        java.util.Iterator<BlockSnapshot> it = deque.descendingIterator();
        while (it.hasNext()) {
            BlockSnapshot s = it.next();
            if (s.x() == x && s.y() == y && s.z() == z && s.blockStateId() == expectedStateId) return true;
        }
        return false;
    }

    private java.util.List<BlockSnapshot> parseBlocksList(JsonObject resp) {
        java.util.List<BlockSnapshot> out = new java.util.ArrayList<>();
        if (resp.has("blocks") && resp.get("blocks").isJsonArray()) {
            for (JsonElement e : resp.getAsJsonArray("blocks")) {
                JsonObject bo = e.getAsJsonObject();
                int x = bo.get("x").getAsInt();
                int y = bo.get("y").getAsInt();
                int z = bo.get("z").getAsInt();
                int stateId = bo.get("stateId").getAsInt();
                out.add(new BlockSnapshot(x, y, z, stateId));
            }
        }
        return java.util.Collections.unmodifiableList(out);
    }

    // ── Player self-state (C) ──────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<BotAbilities> queryAbilities(@NotNull String name, @NotNull Duration timeout) {
        // Always use RPC - the push-event cache is stale by the time a current query arrives.
        // The cache is still populated from RPC responses so subsequent queries can use it.
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_abilities");
        req.addProperty("bot", name);
        return sendRequest(req).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(resp -> {
                    BotAbilities a = new BotAbilities(
                            resp.has("invincible") && resp.get("invincible").getAsBoolean(),
                            resp.has("canFly") && resp.get("canFly").getAsBoolean(),
                            resp.has("flying") && resp.get("flying").getAsBoolean(),
                            resp.has("creative") && resp.get("creative").getAsBoolean(),
                            resp.has("flySpeed") ? resp.get("flySpeed").getAsFloat() : 0f,
                            resp.has("walkSpeed") ? resp.get("walkSpeed").getAsFloat() : 0f);
                    abilitiesCache.put(name, a);
                    return a;
                });
    }

    @Override
    public @NotNull CompletableFuture<Void> respawn(@NotNull String name) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "respawn");
        req.addProperty("bot", name);
        return sendRequest(req).thenAccept(resp -> { /* void */ });
    }

    @Override
    public @NotNull CompletableFuture<Void> sendInput(@NotNull String name,
                                                     boolean forward, boolean backward,
                                                     boolean left, boolean right,
                                                     boolean jump, boolean sneak, boolean sprint) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "player_input");
        req.addProperty("bot", name);
        req.addProperty("forward", forward);
        req.addProperty("backward", backward);
        req.addProperty("left", left);
        req.addProperty("right", right);
        req.addProperty("jump", jump);
        req.addProperty("sneak", sneak);
        req.addProperty("sprint", sprint);
        return sendRequest(req).thenAccept(resp -> { /* void */ });
    }

    @Override
    public @NotNull CompletableFuture<Void> setFlying(@NotNull String name, boolean enable) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "set_flying");
        req.addProperty("bot", name);
        req.addProperty("flying", enable);
        return sendRequest(req).thenAccept(resp -> { /* void */ });
    }

    @Override
    public @NotNull CompletableFuture<Void> useItem(@NotNull String name, @NotNull Bot.Hand hand) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "use_item");
        req.addProperty("bot", name);
        req.addProperty("hand", hand.ordinal());
        return sendRequest(req).thenAccept(resp -> { /* void */ });
    }

    // ── Await primitives ───────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<String> awaitChat(@NotNull String name, @NotNull String text, @NotNull Duration timeout) {
        return awaitEvent(name, event ->
                        "CHAT".equals(event.type()) && event.message() != null && event.message().contains(text),
                timeout)
                .thenApply(BotEventListener.BotEvent::message);
    }

    @Override
    public @NotNull CompletableFuture<String> awaitChat(@NotNull String name, @NotNull Pattern pattern, @NotNull Duration timeout) {
        return awaitEvent(name, event ->
                        "CHAT".equals(event.type()) && event.message() != null && pattern.matcher(event.message()).find(),
                timeout)
                .thenApply(BotEventListener.BotEvent::message);
    }

    @Override
    public @NotNull CompletableFuture<String> awaitAnyChat(@NotNull String name, @NotNull Duration timeout, @NotNull String... texts) {
        return awaitEvent(name, event -> {
            if (!"CHAT".equals(event.type()) || event.message() == null) return false;
            for (String text : texts) {
                if (event.message().contains(text)) return true;
            }
            return false;
        }, timeout).thenApply(BotEventListener.BotEvent::message);
    }

    @Override
    public @NotNull CompletableFuture<Bot.Position> awaitPosition(@NotNull String name, int x, int y, int z, double radius, @NotNull Duration timeout) {
        // Use a periodic poll of query_position against the cache. We accept a
        // small boundary error here: this is a test helper, not a hot path.
        CompletableFuture<Bot.Position> result = new CompletableFuture<>();
        long deadlineMs = System.currentTimeMillis() + timeout.toMillis();
        Runnable poll = new Runnable() {
            @Override public void run() {
                if (result.isDone()) return;
                queryPosition(name, Duration.ofMillis(500)).whenComplete((pos, err) -> {
                    if (result.isDone()) return;
                    if (err != null) {
                        // try again on the next tick
                    } else if (Math.sqrt(Math.pow(pos.x() - x, 2)
                            + Math.pow(pos.y() - y, 2)
                            + Math.pow(pos.z() - z, 2)) <= radius) {
                        result.complete(pos);
                    }
                    if (System.currentTimeMillis() > deadlineMs) {
                        result.completeExceptionally(new RuntimeException(
                                "awaitPosition(" + name + ") timed out after " + timeout.toMillis() + "ms"));
                        return;
                    }
                    if (!result.isDone()) timeoutScheduler.schedule(this, 200, TimeUnit.MILLISECONDS);
                });
            }
        };
        poll.run();
        return result;
    }

    @Override
    public @NotNull CompletableFuture<Void> awaitServer(@NotNull String name, @NotNull String serverName, @NotNull Duration timeout) {
        // Wait for either a "server" event whose message matches the requested
        // server name, or a "disconnected"->"ready" cycle for that target server.
        // The DevRunner's BotTcpServer pushes a "server" event right after the
        // underlying client confirms a transfer (or a fresh connect).
        String target = Objects.requireNonNull(serverName);

        // 1) Check the server-arrival cache first - let pre-existing arrivals
        //    short-circuit even if the await slot is registered after the event
        //    was already dispatched (race condition avoidance).
        String arrived = serverCache.get(name);
        if (arrived != null && (arrived.equals(target) || arrived.equals(target.toLowerCase()))) {
            return CompletableFuture.completedFuture(null);
        }

        // 2) Otherwise subscribe to subsequent SERVER events.
        return awaitEventUnscoped(name, event -> {
            if (!"SERVER".equals(event.type())) return false;
            String m = event.message();
            return m != null && (m.equals(target) || m.equals(target.toLowerCase()));
        }, timeout).thenApply(event -> null);
    }

    @Override
    public @NotNull CompletableFuture<BotEventListener.BotEvent> awaitEvent(
            @NotNull String name,
            @NotNull Predicate<BotEventListener.BotEvent> predicate,
            @NotNull Duration timeout) {
        // Scope the await to the (bot, server) pair this client connected the
        // bot to. The devrunner broadcasts every event to every backend, and
        // cross-server tests connect the same bot name to several servers, so
        // an event tagged with a DIFFERENT server must not satisfy a local
        // await. The scope is the bot's primary server at registration time.
        String scopeServer = botPrimaryServer.get(name);
        EventDispatcher.AwaitSlot slot = new EventDispatcher.AwaitSlot(event ->
                name.equals(event.botName())
                        && serverMatchesScope(event, scopeServer)
                        && predicate.test(event));
        dispatcher.registerAwait(slot);
        return slot.future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .handle((event, ex) -> {
                    dispatcher.removeAwait(slot);
                    if (ex != null) {
                        Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                        if (cause instanceof java.util.concurrent.TimeoutException) {
                            throw new RuntimeException("awaitEvent(" + name + ") timed out after " + timeout.toMillis() + "ms");
                        }
                        throw new RuntimeException("awaitEvent(" + name + ") failed: " + cause.getMessage(), cause);
                    }
                    return event;
                });
    }

    /**
     * {@link #awaitEvent} without the (bot, primary-server) scope. Used by
     * {@link #awaitServer}, whose "server" events are tagged with the bot's
     * CURRENT server - which is exactly what the predicate matches against
     * and may differ from the server the bot was originally connected to
     * (cross-server transfers).
     */
    private @NotNull CompletableFuture<BotEventListener.BotEvent> awaitEventUnscoped(
            @NotNull String name,
            @NotNull Predicate<BotEventListener.BotEvent> predicate,
            @NotNull Duration timeout) {
        EventDispatcher.AwaitSlot slot = new EventDispatcher.AwaitSlot(event ->
                name.equals(event.botName()) && predicate.test(event));
        dispatcher.registerAwait(slot);
        return slot.future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .handle((event, ex) -> {
                    dispatcher.removeAwait(slot);
                    if (ex != null) {
                        Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                        if (cause instanceof java.util.concurrent.TimeoutException) {
                            throw new RuntimeException("awaitEvent(" + name + ") timed out after " + timeout.toMillis() + "ms");
                        }
                        throw new RuntimeException("awaitEvent(" + name + ") failed: " + cause.getMessage(), cause);
                    }
                    return event;
                });
    }

    /**
     * True when the event's server tag matches the server the await was scoped
     * to. Events without a server tag (legacy producers) and awaits for bots
     * with no known primary server are allowed through unchanged.
     */
    private static boolean serverMatchesScope(@NotNull BotEventListener.BotEvent event,
                                              @Nullable String scopeServer) {
        if (scopeServer == null || scopeServer.isEmpty()) return true;
        String evtServer = event.server();
        if (evtServer == null || evtServer.isEmpty()) return true;
        return scopeServer.equals(evtServer);
    }

    @Override
    public void addEventListener(@NotNull BotEventListener listener) {
        dispatcher.addListener(listener);
    }

    @Override
    public void removeEventListener(@NotNull BotEventListener listener) {
        dispatcher.removeListener(listener);
    }

    // ── Inventory packet decoding ────────────────────────────────────────────

    private @Nullable BotInventory getCachedInventory(String botName, int containerId) {
        Map<Integer, BotInventory> byContainer = inventoryCache.get(botName);
        return byContainer != null ? byContainer.get(containerId) : null;
    }

    private BotInventory parseInventoryPayload(JsonObject msg) {
        if (!msg.has("containerId")) return null;
        int containerId = msg.get("containerId").getAsInt();
        int stateId = msg.has("stateId") ? msg.get("stateId").getAsInt() : 0;
        BotItemStack[] slots;
        if (msg.has("slots") && msg.get("slots").isJsonArray()) {
            JsonArray arr = msg.getAsJsonArray("slots");
            slots = new BotItemStack[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                slots[i] = parseItem(arr.get(i));
            }
        } else {
            slots = new BotItemStack[0];
        }
        BotItemStack cursor = msg.has("cursorItem") ? parseItem(msg.get("cursorItem")) : BotItemStack.EMPTY;
        return new BotInventory(containerId, stateId, slots, cursor);
    }

    private static BotItemStack parseItem(JsonElement el) {
        if (el == null || el.isJsonNull() || !el.isJsonObject()) return BotItemStack.EMPTY;
        JsonObject obj = el.getAsJsonObject();
        if (!obj.has("id")) return BotItemStack.EMPTY;
        int id = obj.get("id").getAsInt();
        int amount = obj.has("amount") ? obj.get("amount").getAsInt() : 0;
        String components = obj.has("componentsJson") && !obj.get("componentsJson").isJsonNull()
                ? obj.get("componentsJson").getAsString() : null;
        if (id < 0 || amount <= 0) return BotItemStack.EMPTY;
        return new BotItemStack(id, amount, components);
    }

    // ── Entity cache + parsing ──────────────────────────────────────────────

    private BotEntity findCachedEntity(@NotNull String botName, @NotNull EntityMatcher matcher) {
        Map<Integer, BotEntity> byId = entityCache.get(botName);
        if (byId == null) return null;
        // Lowest entityId first for stable ordering.
        for (Map.Entry<Integer, BotEntity> e : new java.util.TreeMap<>(byId).entrySet()) {
            if (matcher.test(e.getValue())) return e.getValue();
        }
        return null;
    }

    private BotEntity parseEntityPayload(JsonObject msg) {
        if (!msg.has("entityId")) return null;
        int entityId = msg.get("entityId").getAsInt();
        int typeId = msg.has("typeId") ? msg.get("typeId").getAsInt() : -1;
        String typeName = msg.has("typeName") && !msg.get("typeName").isJsonNull()
                ? msg.get("typeName").getAsString() : null;
        double x = msg.has("x") ? msg.get("x").getAsDouble() : 0.0;
        double y = msg.has("y") ? msg.get("y").getAsDouble() : 0.0;
        double z = msg.has("z") ? msg.get("z").getAsDouble() : 0.0;
        float yaw = msg.has("yaw") ? msg.get("yaw").getAsFloat() : 0f;
        float pitch = msg.has("pitch") ? msg.get("pitch").getAsFloat() : 0f;
        float headYaw = msg.has("headYaw") ? msg.get("headYaw").getAsFloat() : yaw;
        return new BotEntity(entityId, typeId, typeName, x, y, z, yaw, pitch, headYaw);
    }

    @Nullable
    private BotAbilities parseAbilitiesPayload(JsonObject msg) {
        if (!msg.has("creative") && !msg.has("invincible") && !msg.has("canFly")) return null;
        return new BotAbilities(
                msg.has("invincible") && msg.get("invincible").getAsBoolean(),
                msg.has("canFly") && msg.get("canFly").getAsBoolean(),
                msg.has("flying") && msg.get("flying").getAsBoolean(),
                msg.has("creative") && msg.get("creative").getAsBoolean(),
                msg.has("flySpeed") ? msg.get("flySpeed").getAsFloat() : 0f,
                msg.has("walkSpeed") ? msg.get("walkSpeed").getAsFloat() : 0f);
    }

    private java.util.List<BotEntity> parseEntityList(JsonObject resp, @NotNull EntityMatcher matcher) {
        java.util.List<BotEntity> out = new java.util.ArrayList<>();
        if (resp.has("entities") && resp.get("entities").isJsonArray()) {
            for (JsonElement e : resp.getAsJsonArray("entities")) {
                BotEntity ent = parseEntityPayload(e.getAsJsonObject());
                if (ent != null && matcher.test(ent)) out.add(ent);
            }
        }
        return java.util.Collections.unmodifiableList(out);
    }

    private BotEntity parseEntity(JsonObject resp, int expectedId) {
        if (resp.has("entities") && resp.get("entities").isJsonArray()) {
            for (JsonElement e : resp.getAsJsonArray("entities")) {
                BotEntity ent = parseEntityPayload(e.getAsJsonObject());
                if (ent != null && ent.entityId() == expectedId) return ent;
            }
        }
        // Fallback: top-level fields
        if (resp.has("entityId")) {
            return parseEntityPayload(resp);
        }
        return BotEntity.unknown(expectedId);
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    private static ScheduledExecutorService newTimeoutScheduler() {
        ScheduledThreadPoolExecutor ex = new ScheduledThreadPoolExecutor(
                1, r -> {
            Thread t = new Thread(r, "rpc-bot-timeout");
            t.setDaemon(true);
            return t;
        });
        ex.setRemoveOnCancelPolicy(true);
        Runtime.getRuntime().addShutdownHook(new Thread(ex::shutdownNow, "rpc-bot-timeout-hook"));
        return ex;
    }

    @Override
    public void close() {
        running = false;
        try { socket.close(); } catch (IOException ignored) {}
        timeoutScheduler.shutdownNow();
    }

    // ── Errors ──────────────────────────────────────────────────────────────

    /** Typed exception surfaced by the server's {@code "error"} response type. */
    public static final class BotRpcException extends RuntimeException {
        @Nullable private final String errorType;

        BotRpcException(@NotNull String message, @Nullable String errorType) {
            super(message);
            this.errorType = errorType;
        }

        @Nullable public String errorType() { return errorType; }
    }
}
