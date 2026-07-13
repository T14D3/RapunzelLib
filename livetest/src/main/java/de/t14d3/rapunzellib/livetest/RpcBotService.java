package de.t14d3.rapunzellib.livetest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * A {@link BotService} implementation that communicates with the DevRunner's
 * {@code BotTcpServer} over a TCP socket using a JSON-line protocol.
 *
 * <p>This replaces the stdout-based {@link SharedConsoleBotService} when running
 * under the DevRunner with the TCP bot transport enabled. The TCP port is read
 * from the {@code rapunzellib.bot.rpc.port} system property.</p>
 *
 * <p>Each instance opens one persistent TCP connection to the DevRunner and
 * multiplexes requests for multiple bots over that connection. Responses are
 * correlated by request ID.</p>
 */
public class RpcBotService implements BotService, AutoCloseable {

    private static final Gson GSON = new Gson();
    private static final String PORT_PROPERTY = "rapunzellib.bot.rpc.port";
    private static final int DEFAULT_PORT = 26566;

    private final Socket socket;
    private final PrintWriter writer;
    private final BufferedReader reader;
    private final AtomicLong requestIdCounter = new AtomicLong(1);
    private final Map<Long, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    private final List<BotEventListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean running = true;

    /**
     * Creates an RPC bot service connecting to localhost on the port specified
     * by the {@code rapunzellib.bot.rpc.port} system property (default 26566).
     */
    public RpcBotService() {
        this("127.0.0.1", Integer.getInteger(PORT_PROPERTY, DEFAULT_PORT));
    }

    /**
     * Creates an RPC bot service connecting to the given host and port.
     *
     * @param host the DevRunner host
     * @param port the DevRunner TCP port
     */
    public RpcBotService(String host, int port) {
        try {
            this.socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5_000);
            socket.setSoTimeout(30_000);
            this.writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            this.reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to BotRPC server at " + host + ":" + port, e);
        }
        Thread readerThread = new Thread(this::readLoop, "rpc-bot-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private CompletableFuture<JsonObject> sendRequest(JsonObject request) {
        long id = requestIdCounter.getAndIncrement();
        request.addProperty("id", id);
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pending.put(id, future);
        synchronized (this) {
            writer.println(GSON.toJson(request));
        }
        return future;
    }

    private void sendAndForget(JsonObject request) {
        long id = requestIdCounter.getAndIncrement();
        request.addProperty("id", id);
        synchronized (this) {
            writer.println(GSON.toJson(request));
        }
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    JsonObject msg = GSON.fromJson(line, JsonObject.class);
                    if (msg == null) continue;

                    String type = msg.get("type").getAsString();

                    if ("ok".equals(type) || "error".equals(type) ||
                            "position".equals(type) || "health".equals(type) ||
                            "held_item".equals(type) || "gamemode".equals(type) ||
                            "open_container".equals(type) || "entities".equals(type)) {
                        // Response to a request - complete the pending future
                        long id = msg.get("id").getAsLong();
                        CompletableFuture<JsonObject> future = pending.remove(id);
                        if (future != null) {
                            if ("error".equals(type)) {
                                String errMsg = msg.has("message") ? msg.get("message").getAsString() : "Unknown error";
                                future.completeExceptionally(new RuntimeException(errMsg));
                            } else {
                                future.complete(msg);
                            }
                        }
                    } else if ("chat".equals(type) || "ready".equals(type) || "disconnected".equals(type)) {
                        // Event pushed from DevRunner - dispatch to listeners
                        String botName = msg.get("bot").getAsString();
                        String message = msg.has("message") ? msg.get("message").getAsString() : "";
                        BotEventListener.BotEvent event = new BotEventListener.BotEvent(
                                type.toUpperCase(), botName, message);
                        for (BotEventListener listener : listeners) {
                            try {
                                listener.onBotEvent(event);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                } catch (Exception e) {
                    // Log and continue
                }
            }
        } catch (IOException e) {
            if (running) {
                // Connection lost - fail all pending futures
                for (CompletableFuture<JsonObject> f : pending.values()) {
                    f.completeExceptionally(new RuntimeException("BotRPC connection lost", e));
                }
                pending.clear();
            }
        }
    }

    // ── BotService implementation ────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<Bot> connect(@NotNull String name, @NotNull String serverName) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "connect");
        req.addProperty("bot", name);
        req.addProperty("server", serverName);

        return sendRequest(req).thenApply(resp -> {
            return new Bot(name, serverName, this);
        });
    }

    @Override
    public void disconnect(@NotNull String name) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "disconnect");
        req.addProperty("bot", name);
        sendAndForget(req);
    }

    @Override
    public void execute(@NotNull String name, @NotNull String command) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "exec");
        req.addProperty("bot", name);
        req.addProperty("command", command);
        sendAndForget(req);
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
        return sendRequest(req).orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
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
        return sendRequest(req).orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
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
        return sendRequest(req).orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .thenApply(resp -> new Bot.HeldItem(
                        resp.get("slot").getAsInt(),
                        resp.has("material") ? resp.get("material").getAsString() : "unknown"));
    }

    @Override
    public @NotNull CompletableFuture<String> queryGameMode(@NotNull String name, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_gamemode");
        req.addProperty("bot", name);
        return sendRequest(req).orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .thenApply(resp -> resp.get("gamemode").getAsString());
    }

    @Override
    public @NotNull CompletableFuture<Integer> queryOpenContainer(@NotNull String name, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_open_container");
        req.addProperty("bot", name);
        return sendRequest(req).orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .thenApply(resp -> resp.get("containerId").getAsInt());
    }

    @Override
    public @NotNull CompletableFuture<int[]> queryEntities(@NotNull String name, @NotNull String typeName, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_entities");
        req.addProperty("bot", name);
        req.addProperty("entityType", typeName);
        return sendRequest(req).orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .thenApply(resp -> {
                    JsonArray arr = resp.getAsJsonArray("entityIds");
                    int[] result = new int[arr.size()];
                    for (int i = 0; i < arr.size(); i++) result[i] = arr.get(i).getAsInt();
                    return result;
                });
    }

    // ── Await methods (via event polling) ───────────────────────────────────

    @Override
    public @NotNull CompletableFuture<String> awaitChat(@NotNull String name, @NotNull String text, @NotNull Duration timeout) {
        return pollEvent("CHAT", name, timeout, (event) ->
                event.message() != null && event.message().contains(text));
    }

    @Override
    public @NotNull CompletableFuture<String> awaitChat(@NotNull String name, @NotNull Pattern pattern, @NotNull Duration timeout) {
        return pollEvent("CHAT", name, timeout, (event) ->
                event.message() != null && pattern.matcher(event.message()).find());
    }

    @Override
    public @NotNull CompletableFuture<String> awaitAnyChat(@NotNull String name, @NotNull Duration timeout, @NotNull String... texts) {
        return pollEvent("CHAT", name, timeout, (event) -> {
            if (event.message() == null) return false;
            for (String text : texts) {
                if (event.message().contains(text)) return true;
            }
            return false;
        });
    }

    @Override
    public @NotNull CompletableFuture<Bot.Position> awaitPosition(@NotNull String name, int x, int y, int z, double radius, @NotNull Duration timeout) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "query_position");
        req.addProperty("bot", name);
        return sendRequest(req).orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .thenApply(resp -> {
                    double rx = resp.get("x").getAsDouble();
                    double ry = resp.get("y").getAsDouble();
                    double rz = resp.get("z").getAsDouble();
                    double dist = Math.sqrt(Math.pow(rx - x, 2) + Math.pow(ry - y, 2) + Math.pow(rz - z, 2));
                    if (dist > radius) {
                        throw new RuntimeException("Bot '" + name + "' not at position (" + x + "," + y + "," + z
                                + "): distance " + String.format("%.2f", dist) + " > " + radius);
                    }
                    return new Bot.Position(rx, ry, rz,
                            resp.get("yaw").getAsFloat(), resp.get("pitch").getAsFloat());
                });
    }

    @Override
    public @NotNull CompletableFuture<Void> awaitServer(@NotNull String name, @NotNull String serverName, @NotNull Duration timeout) {
        return CompletableFuture.completedFuture(null);
    }

    // ── Event listener management ──────────────────────────────────────────

    @Override
    public void addEventListener(@NotNull BotEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeEventListener(@NotNull BotEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void injectEvent(@NotNull String type, @NotNull String botName, @NotNull String message) {
        BotEventListener.BotEvent event = new BotEventListener.BotEvent(type, botName, message);
        for (BotEventListener listener : listeners) {
            try {
                listener.onBotEvent(event);
            } catch (Exception ignored) {
            }
        }
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private CompletableFuture<String> pollEvent(String type, String botName, Duration timeout,
                                                  java.util.function.Predicate<BotEventListener.BotEvent> matcher) {
        return CompletableFuture.supplyAsync(() -> {
            Instant deadline = Instant.now().plus(timeout);
            java.util.Queue<BotEventListener.BotEvent> eventQueue =
                    new java.util.concurrent.ConcurrentLinkedQueue<>();

            BotEventListener listener = event -> {
                if (event.type().equals(type) && event.botName().equals(botName)) {
                    eventQueue.add(event);
                }
            };
            addEventListener(listener);
            try {
                while (Instant.now().isBefore(deadline)) {
                    BotEventListener.BotEvent event = eventQueue.poll();
                    if (event != null && matcher.test(event)) {
                        return event.message();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while polling for " + type, e);
                    }
                }
                throw new RuntimeException("Timeout waiting for " + type + " from bot '" + botName + "'");
            } finally {
                removeEventListener(listener);
            }
        });
    }

    private static int getServerPort(String serverName) {
        // Default lobby port used by DevRunner
        return 25565;
    }

    @Override
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}