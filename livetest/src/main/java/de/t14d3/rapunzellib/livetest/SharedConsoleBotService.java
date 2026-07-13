package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Console-based {@link BotService} implementation that prints bot commands to stdout
 * and polls an in-memory event queue.
 * <p>
 * This service is used as a fallback when no platform-specific {@link BotService}
 * is registered. Events can be injected into the queue via
 * {@link BotFactory#addEvent(BotFactory.BotEvent)}.
 * </p>
 */
public class SharedConsoleBotService implements BotService {

    private static final long DEFAULT_POLL_INTERVAL_MS = 100L;

    private final Queue<BotFactory.BotEvent> eventQueue;
    private final List<BotEventListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates a console bot service with a shared event queue.
     *
     * @param eventQueue the event queue to poll for bot events
     */
    public SharedConsoleBotService(Queue<BotFactory.BotEvent> eventQueue) {
        this.eventQueue = eventQueue;
    }

    /**
     * Creates a console bot service with an independent event queue.
     */
    public SharedConsoleBotService() {
        this(new ConcurrentLinkedQueue<>());
    }

    // ── Fire-and-forget ─────────────────────────────────────────────────────

    @Override
    public void digBlock(@NotNull String name, int x, int y, int z, @NotNull Bot.Direction direction) {
        System.out.println("[BOT_DIG] " + name + " " + x + " " + y + " " + z + " " + direction.ordinal());
    }

    @Override
    public void useItemOn(@NotNull String name, int x, int y, int z, @NotNull Bot.Hand hand, @NotNull Bot.Direction direction) {
        System.out.println("[BOT_USE] " + name + " " + x + " " + y + " " + z + " " + hand.ordinal() + " " + direction.ordinal());
    }

    @Override
    public void execute(@NotNull String name, @NotNull String command) {
        System.out.println("[BOT_EXEC] " + name + " " + command);
    }

    @Override
    public void disconnect(@NotNull String name) {
        System.out.println("[BOT_DISCONNECT] " + name);
    }

    @Override
    public void moveTo(@NotNull String name, int x, int y, int z) {
        System.out.println("[BOT_MOVE_TO] " + name + " " + x + " " + y + " " + z);
    }

    @Override
    public void attackEntity(@NotNull String name, int entityId) {
        System.out.println("[BOT_ATTACK] " + name + " " + entityId);
    }

    @Override
    public void interactEntity(@NotNull String name, int entityId, int hand) {
        System.out.println("[BOT_INTERACT] " + name + " " + entityId + " " + hand);
    }

    @Override
    public void swingHand(@NotNull String name, int hand) {
        System.out.println("[BOT_SWING] " + name + " " + hand);
    }

    @Override
    public void setHeldItemSlot(@NotNull String name, int slot) {
        System.out.println("[BOT_SET_SLOT] " + name + " " + slot);
    }

    // ── Async (blocking polling wrapped in CompletableFuture) ───────────────

    @Override
    public @NotNull CompletableFuture<Bot> connect(@NotNull String name, @NotNull String serverName) {
        System.out.println("[BOT_CONNECT] " + name + " " + serverName);
        return CompletableFuture.supplyAsync(() -> {
            long timeoutMs = 60_000L;
            var deadline = Instant.now().plusMillis(timeoutMs);
            while (Instant.now().isBefore(deadline)) {
                BotFactory.BotEvent event = pollEvent("READY", name);
                if (event != null) {
                    return new Bot(name, serverName, this);
                }
                BotFactory.BotEvent error = pollEvent("ERROR", name);
                if (error != null) throw new RuntimeException("Bot connection failed: " + error.message());
                sleep(DEFAULT_POLL_INTERVAL_MS);
            }
            throw new RuntimeException("Timeout waiting for bot '" + name + "' to connect to '" + serverName + "'");
        });
    }

    @Override
    public @NotNull CompletableFuture<String> awaitChat(
            @NotNull String name, @NotNull String text, @NotNull Duration timeout
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                BotFactory.BotEvent event = pollEvent("CHAT", name);
                if (event != null && event.message() != null && event.message().contains(text)) {
                    return event.message();
                }
                sleep(DEFAULT_POLL_INTERVAL_MS);
            }
            throw new RuntimeException("Timeout waiting for chat from bot '" + name + "' containing '" + text + "'");
        });
    }

    @Override
    public @NotNull CompletableFuture<String> awaitChat(
            @NotNull String name, @NotNull Pattern pattern, @NotNull Duration timeout
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                BotFactory.BotEvent event = pollEvent("CHAT", name);
                if (event != null && event.message() != null && pattern.matcher(event.message()).find()) {
                    return event.message();
                }
                sleep(DEFAULT_POLL_INTERVAL_MS);
            }
            throw new RuntimeException("Timeout waiting for chat from bot '" + name + "' matching '" + pattern + "'");
        });
    }

    @Override
    public @NotNull CompletableFuture<String> awaitAnyChat(
            @NotNull String name, @NotNull Duration timeout, @NotNull String... texts
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                BotFactory.BotEvent event = pollEvent("CHAT", name);
                if (event != null && event.message() != null) {
                    for (String text : texts) {
                        if (event.message().contains(text)) return event.message();
                    }
                }
                sleep(DEFAULT_POLL_INTERVAL_MS);
            }
            throw new RuntimeException("Timeout waiting for chat from bot '"
                    + name + "' containing one of " + java.util.Arrays.toString(texts));
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> awaitServer(
            @NotNull String name, @NotNull String serverName, @NotNull Duration timeout
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                BotFactory.BotEvent event = pollEvent("SERVER", name);
                if (event != null && serverName.equals(event.message())) return null;
                sleep(DEFAULT_POLL_INTERVAL_MS);
            }
            throw new RuntimeException("Timeout waiting for bot '" + name + "' to be on server '" + serverName + "'");
        });
    }

    // ── State queries ───────────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<Bot.Position> queryPosition(
            @NotNull String name, @NotNull Duration timeout
    ) {
        clearEvents("POSITION", name);
        System.out.println("[BOT_QUERY_POSITION] " + name);
        return CompletableFuture.supplyAsync(() -> {
            var deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                BotFactory.BotEvent event = pollEvent("POSITION", name);
                if (event != null && event.message() != null && !event.message().isEmpty()) {
                    String[] parts = event.message().split(" ");
                    if (parts.length >= 5) {
                        return new Bot.Position(
                                Double.parseDouble(parts[0]),
                                Double.parseDouble(parts[1]),
                                Double.parseDouble(parts[2]),
                                Float.parseFloat(parts[3]),
                                Float.parseFloat(parts[4])
                        );
                    }
                }
                sleep(DEFAULT_POLL_INTERVAL_MS);
            }
            throw new RuntimeException("Timeout waiting for position of bot '" + name + "'");
        });
    }

    @Override
    public @NotNull CompletableFuture<Bot.Health> queryHealth(
            @NotNull String name, @NotNull Duration timeout
    ) {
        clearEvents("HEALTH", name);
        System.out.println("[BOT_QUERY_HEALTH] " + name);
        return CompletableFuture.supplyAsync(() -> {
            var deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                BotFactory.BotEvent event = pollEvent("HEALTH", name);
                if (event != null && event.message() != null && !event.message().isEmpty()) {
                    String[] parts = event.message().split(" ");
                    if (parts.length >= 3) {
                        return new Bot.Health(
                                Float.parseFloat(parts[0]),
                                Integer.parseInt(parts[1]),
                                Float.parseFloat(parts[2])
                        );
                    }
                }
                sleep(DEFAULT_POLL_INTERVAL_MS);
            }
            throw new RuntimeException("Timeout waiting for health of bot '" + name + "'");
        });
    }

    @Override
    public @NotNull CompletableFuture<Bot.HeldItem> queryHeldItem(
            @NotNull String name, @NotNull Duration timeout
    ) {
        clearEvents("HELD_ITEM", name);
        System.out.println("[BOT_QUERY_HELD_ITEM] " + name);
        return CompletableFuture.supplyAsync(() -> {
            var deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                BotFactory.BotEvent event = pollEvent("HELD_ITEM", name);
                if (event != null && event.message() != null && !event.message().isEmpty()) {
                    String[] parts = event.message().split(" ");
                    int slot = Integer.parseInt(parts[0]);
                    String material = parts.length > 1 ? parts[1] : "unknown";
                    return new Bot.HeldItem(slot, material);
                }
                sleep(DEFAULT_POLL_INTERVAL_MS);
            }
            throw new RuntimeException("Timeout waiting for held item of bot '" + name + "'");
        });
    }

    @Override
    public @NotNull CompletableFuture<String> queryGameMode(
            @NotNull String name, @NotNull Duration timeout
    ) {
        clearEvents("GAMEMODE", name);
        System.out.println("[BOT_QUERY_GAMEMODE] " + name);
        return CompletableFuture.supplyAsync(() -> {
            var deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                BotFactory.BotEvent event = pollEvent("GAMEMODE", name);
                if (event != null && event.message() != null && !event.message().isEmpty()) {
                    return event.message().trim();
                }
                sleep(DEFAULT_POLL_INTERVAL_MS);
            }
            throw new RuntimeException("Timeout waiting for game mode of bot '" + name + "'");
        });
    }

    @Override
    public @NotNull CompletableFuture<Integer> queryOpenContainer(
            @NotNull String name, @NotNull Duration timeout
    ) {
        clearEvents("OPEN_CONTAINER", name);
        System.out.println("[BOT_QUERY_OPEN_CONTAINER] " + name);
        return CompletableFuture.supplyAsync(() -> {
            var deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                BotFactory.BotEvent event = pollEvent("OPEN_CONTAINER", name);
                if (event != null && event.message() != null && !event.message().isEmpty()) {
                    return Integer.parseInt(event.message().trim());
                }
                sleep(DEFAULT_POLL_INTERVAL_MS);
            }
            throw new RuntimeException("Timeout waiting for open container of bot '" + name + "'");
        });
    }

    @Override
    public @NotNull CompletableFuture<int[]> queryEntities(
            @NotNull String name, @NotNull String typeName, @NotNull Duration timeout
    ) {
        clearEvents("ENTITIES", name);
        System.out.println("[BOT_QUERY_ENTITIES] " + name + " " + typeName);
        return CompletableFuture.supplyAsync(() -> {
            var deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                BotFactory.BotEvent event = pollEvent("ENTITIES", name);
                if (event != null && event.message() != null && !event.message().isEmpty()) {
                    String[] parts = event.message().split(" ");
                    int[] ids = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        ids[i] = Integer.parseInt(parts[i]);
                    }
                    return ids;
                }
                sleep(DEFAULT_POLL_INTERVAL_MS);
            }
            throw new RuntimeException("Timeout waiting for entities of bot '" + name + "'");
        });
    }

    @Override
    public @NotNull CompletableFuture<Bot.Position> awaitPosition(
            @NotNull String name, int x, int y, int z, double radius, @NotNull Duration timeout
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                try {
                    Bot.Position pos = queryPositionImmediate(name, Duration.ofMillis(
                            Math.min(timeout.toMillis(), 5000)));
                    double dx = pos.x() - x;
                    double dy = pos.y() - y;
                    double dz = pos.z() - z;
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance <= radius) {
                        return pos;
                    }
                } catch (Exception e) {
                    // Ignore timeout from the inner query, keep polling
                }
                sleep(200);
            }
            throw new RuntimeException("Timeout waiting for bot '" + name
                    + "' to reach position (" + x + "," + y + "," + z + ")");
        });
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
        eventQueue.add(new BotFactory.BotEvent(type, botName, message));
        BotEventListener.BotEvent listenerEvent = new BotEventListener.BotEvent(type, botName, message);
        for (BotEventListener listener : listeners) {
            listener.onBotEvent(listenerEvent);
        }
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private Bot.Position queryPositionImmediate(String name, Duration timeout) {
        clearEvents("POSITION", name);
        System.out.println("[BOT_QUERY_POSITION] " + name);
        var deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            BotFactory.BotEvent event = pollEvent("POSITION", name);
            if (event != null && event.message() != null && !event.message().isEmpty()) {
                String[] parts = event.message().split(" ");
                if (parts.length >= 5) {
                    return new Bot.Position(
                            Double.parseDouble(parts[0]),
                            Double.parseDouble(parts[1]),
                            Double.parseDouble(parts[2]),
                            Float.parseFloat(parts[3]),
                            Float.parseFloat(parts[4])
                    );
                }
            }
            sleep(DEFAULT_POLL_INTERVAL_MS);
        }
        throw new RuntimeException("Timeout waiting for position of bot '" + name + "'");
    }

    private BotFactory.BotEvent pollEvent(String type, String botName) {
        for (BotFactory.BotEvent e : eventQueue) {
            if (e.type().equals(type) && e.botName().equals(botName)) {
                eventQueue.remove(e);
                return e;
            }
        }
        return null;
    }

    private void clearEvents(String type, String botName) {
        eventQueue.removeIf(e -> e.type().equals(type) && e.botName().equals(botName));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while polling", e);
        }
    }
}
