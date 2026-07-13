package de.t14d3.rapunzellib.devrunner.bot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * TCP server that receives bot commands from the server-side {@code RpcBotService}
 * and dispatches them to a {@link BotManager}.
 *
 * <p>Protocol: JSON lines over TCP. Each line is a complete JSON object.
 * Requests have an {@code "id"} field for correlation; responses echo it back.
 * The server runs on a configurable port and accepts multiple concurrent connections.</p>
 */
public class BotTcpServer implements AutoCloseable {

    private static final Gson GSON = new Gson();

    /** Resolves a server name (e.g. "lobby") to a "host:port" address. */
    @FunctionalInterface
    public interface ServerAddressResolver {
        /** @return "host:port" string, or null if unknown */
        String resolve(String serverName);
    }

    private final BotManager botManager;
    private final int port;
    private final ServerAddressResolver addressResolver;
    private final ExecutorService executor;
    private ServerSocket serverSocket;
    private volatile boolean running;

    /** Registered response handlers keyed by request id. */
    private final Map<Long, Consumer<JsonObject>> pendingResponses = new ConcurrentHashMap<>();
    private long nextId;

    public BotTcpServer(BotManager botManager, int port, ServerAddressResolver addressResolver) {
        this.botManager = botManager;
        this.port = port;
        this.addressResolver = addressResolver;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "bot-tcp-server");
            t.setDaemon(true);
            return t;
        });
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
        try (client;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {

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
        }
    }

    private JsonObject handleRequest(JsonObject request) {
        String type = request.get("type").getAsString();
        String botName = request.has("bot") ? request.get("bot").getAsString() : "";
        long id = request.has("id") ? request.get("id").getAsLong() : -1;

        try {
            switch (type) {
                case "connect" -> {
                    String server = request.get("server").getAsString();
                    String address = addressResolver.resolve(server);
                    if (address == null) {
                        return error(id, "Unknown server: " + server);
                    }
                    String[] parts = address.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    botManager.connectBot(botName, host, port);
                    return ok(id);
                }
                case "disconnect" -> {
                    botManager.disconnectBot(botName);
                    return ok(id);
                }
                case "dig" -> {
                    int x = request.get("x").getAsInt();
                    int y = request.get("y").getAsInt();
                    int z = request.get("z").getAsInt();
                    int dir = request.get("dir").getAsInt();
                    botManager.digBlock(botName, x, y, z, dir);
                    return ok(id);
                }
                case "use" -> {
                    int x = request.get("x").getAsInt();
                    int y = request.get("y").getAsInt();
                    int z = request.get("z").getAsInt();
                    int hand = request.get("hand").getAsInt();
                    int dir = request.get("dir").getAsInt();
                    botManager.useItemOn(botName, x, y, z, hand, dir);
                    return ok(id);
                }
                case "exec" -> {
                    String cmd = request.get("command").getAsString();
                    botManager.execute(botName, cmd);
                    return ok(id);
                }
                case "move_to" -> {
                    int x = request.get("x").getAsInt();
                    int y = request.get("y").getAsInt();
                    int z = request.get("z").getAsInt();
                    botManager.moveTo(botName, x, y, z);
                    return ok(id);
                }
                case "attack" -> {
                    int entityId = request.get("entityId").getAsInt();
                    botManager.attackEntity(botName, entityId);
                    return ok(id);
                }
                case "interact" -> {
                    int entityId = request.get("entityId").getAsInt();
                    int hand = request.get("hand").getAsInt();
                    botManager.interactEntity(botName, entityId, hand);
                    return ok(id);
                }
                case "swing" -> {
                    int hand = request.get("hand").getAsInt();
                    botManager.swingHand(botName, hand);
                    return ok(id);
                }
                case "set_slot" -> {
                    int slot = request.get("slot").getAsInt();
                    botManager.setHeldItemSlot(botName, slot);
                    return ok(id);
                }
                case "query_position" -> {
                    double[] pos = botManager.queryPosition(botName);
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
                    float[] health = botManager.queryHealth(botName);
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
                    int[] item = botManager.queryHeldItem(botName);
                    if (item == null) return error(id, "Bot not found");
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "held_item");
                    res.addProperty("id", id);
                    res.addProperty("slot", item[0]);
                    return res;
                }
                case "query_gamemode" -> {
                    String gm = botManager.queryGameMode(botName);
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "gamemode");
                    res.addProperty("id", id);
                    res.addProperty("gamemode", gm);
                    return res;
                }
                case "query_open_container" -> {
                    int containerId = botManager.queryOpenContainerId(botName);
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "open_container");
                    res.addProperty("id", id);
                    res.addProperty("containerId", containerId);
                    return res;
                }
                case "query_entities" -> {
                    String typeName = request.get("entityType").getAsString();
                    int[] ids = botManager.findEntities(botName, typeName);
                    JsonObject res = new JsonObject();
                    res.addProperty("type", "entities");
                    res.addProperty("id", id);
                    var arr = new com.google.gson.JsonArray();
                    for (int eid : ids) arr.add(eid);
                    res.add("entityIds", arr);
                    return res;
                }
                default -> {
                    return error(id, "Unknown request type: " + type);
                }
            }
        } catch (Exception e) {
            return error(id, e.getMessage());
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
