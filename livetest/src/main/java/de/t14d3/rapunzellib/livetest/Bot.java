package de.t14d3.rapunzellib.livetest;

import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A connected Minecraft bot client used in live tests.
 * <p>
 * Provides methods for controlling and querying an automated bot client
 * that simulates player actions during testing. Bots are typically separate
 * Minecraft client processes.
 * </p>
 * <p>
 * Instances are created via {@link Bot#connect(String, String)} and should
 * be closed via try-with-resources to ensure clean disconnection.
 * </p>
 */
public class Bot implements AutoCloseable {

    public record Position(double x, double y, double z, float yaw, float pitch) {}

    public record Health(float health, int food, float saturation) {}

    public record HeldItem(int slot, String material) {}

    public enum Direction { DOWN, UP, NORTH, SOUTH, WEST, EAST }

    public enum Hand { MAIN_HAND, OFF_HAND }

    private final String name;
    private final String server;
    private final BotService service;
    private final List<BotEventListener> listeners = new CopyOnWriteArrayList<>();

    // ── Factory ────────────────────────────────────────────────────────────

    private static BotService resolveService() {
        Optional<BotService> contextService = Rapunzel.findContext()
                .flatMap(ctx -> ctx.services().find(BotService.class));
        return contextService.orElseGet(BotFactory::newConsoleService);
    }

    /**
     * Connects a bot to the specified server.
     * <p>
     * The returned future completes when the bot is ready for use. The
     * resulting {@link Bot} should be used in a try-with-resources block
     * to ensure clean disconnection.
     * </p>
     *
     * @param name       the bot's player name
     * @param serverName the server address or name to connect to
     * @return a future that completes with a connected bot
     */
    public static @NotNull CompletableFuture<Bot> connect(
            @NotNull String name, @NotNull String serverName
    ) {
        BotService svc = resolveService();
        return svc.connect(name, serverName);
    }

    /**
     * Creates a new bot instance backed by the given service.
     * Package-private - use {@link #connect(String, String)} instead.
     */
    Bot(@NotNull String name, @NotNull String server, @NotNull BotService service) {
        this.name = Objects.requireNonNull(name, "name");
        this.server = Objects.requireNonNull(server, "server");
        this.service = Objects.requireNonNull(service, "service");
    }

    // ── Identity ───────────────────────────────────────────────────────────

    public @NotNull String name() { return name; }

    public @NotNull String server() { return server; }

    // ── Fire-and-forget operations ────────────────────────────────────────

    public void digBlock(int x, int y, int z, @NotNull Direction direction) {
        service.digBlock(name, x, y, z, direction);
    }

    public void useItemOn(int x, int y, int z, @NotNull Hand hand, @NotNull Direction direction) {
        service.useItemOn(name, x, y, z, hand, direction);
    }

    public void execute(@NotNull String command) {
        service.execute(name, command);
    }

    public void moveTo(int x, int y, int z) {
        service.moveTo(name, x, y, z);
    }

    /**
     * Attacks an entity by its entity ID.
     * <p>
     * Sends a left-click attack packet followed by a swing animation.
     * The bot must be close enough to the entity for the attack to register.
     * </p>
     *
     * @param entityId the entity ID to attack
     */
    public void attackEntity(int entityId) {
        service.attackEntity(name, entityId);
    }

    /**
     * Interacts (right-clicks) with an entity by its entity ID.
     * <p>
     * Sends an interact packet followed by a swing animation.
     * The bot must be close enough to the entity for the interaction to register.
     * </p>
     *
     * @param entityId the entity ID to interact with
     * @param hand     the hand to use
     */
    public void interactEntity(int entityId, @NotNull Hand hand) {
        service.interactEntity(name, entityId, hand.ordinal());
    }

    /**
     * Swings the bot's hand (animation only, no attack/interact).
     *
     * @param hand the hand to swing
     */
    public void swingHand(@NotNull Hand hand) {
        service.swingHand(name, hand.ordinal());
    }

    /**
     * Sets the bot's held item slot on the hotbar.
     * <p>
     * Slot 0 is the first hotbar slot, slot 8 is the last.
     * Use this to switch to a specific item before using
     * {@link #useItemOn(int, int, int, Hand, Direction)} or
     * {@link #attackEntity(int)}.
     * </p>
     *
     * @param slot the hotbar slot (0-8)
     */
    public void setHeldItemSlot(int slot) {
        service.setHeldItemSlot(name, slot);
    }

    // ── State query methods (blocking) ─────────────────────────────────────

    public @NotNull Position queryPosition(long timeoutMs) throws Exception {
        return service.queryPosition(name, Duration.ofMillis(timeoutMs))
                .get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public @NotNull String queryGameMode(long timeoutMs) throws Exception {
        return service.queryGameMode(name, Duration.ofMillis(timeoutMs))
                .get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Queries the bot's currently open container ID.
     *
     * @param timeoutMs the maximum time to wait in milliseconds
     * @return the container ID, or -1 if no container is open
     * @throws Exception if the query times out
     */
    public int queryOpenContainer(long timeoutMs) throws Exception {
        return service.queryOpenContainer(name, Duration.ofMillis(timeoutMs))
                .get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Queries entity IDs tracked by the bot matching the given type name.
     * <p>
     * Entity tracking is populated from {@code ClientboundAddEntityPacket}
     * received by the bot client. Use this to discover entity IDs needed
     * for {@link #attackEntity(int)} and {@link #interactEntity(int, Hand)}.
     * </p>
     *
     * @param typeName the entity type name (e.g. {@code "ZOMBIE"}, {@code "SHEEP"})
     * @param timeoutMs the maximum time to wait in milliseconds
     * @return an array of matching entity IDs (may be empty)
     * @throws Exception if the query times out
     */
    public int[] findEntities(@NotNull String typeName, long timeoutMs) throws Exception {
        return service.queryEntities(name, typeName, Duration.ofMillis(timeoutMs))
                .get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public @NotNull Health queryHealth(long timeoutMs) throws Exception {
        return service.queryHealth(name, Duration.ofMillis(timeoutMs))
                .get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public @NotNull HeldItem queryHeldItem(long timeoutMs) throws Exception {
        return service.queryHeldItem(name, Duration.ofMillis(timeoutMs))
                .get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    // ── Async queries ─────────────────────────────────────────────────────

    public @NotNull CompletableFuture<String> awaitChat(@NotNull String text, long timeoutMs) {
        return service.awaitChat(name, text, Duration.ofMillis(timeoutMs));
    }

    public @NotNull CompletableFuture<String> awaitChat(@NotNull Pattern pattern, long timeoutMs) {
        return service.awaitChat(name, pattern, Duration.ofMillis(timeoutMs));
    }

    public @NotNull CompletableFuture<String> awaitAnyChat(long timeoutMs, @NotNull String... texts) {
        return service.awaitAnyChat(name, Duration.ofMillis(timeoutMs), texts);
    }

    public @NotNull CompletableFuture<Position> awaitPosition(int x, int y, int z, int radius, long timeoutMs) {
        return service.awaitPosition(name, x, y, z, radius, Duration.ofMillis(timeoutMs));
    }

    public @NotNull CompletableFuture<Void> awaitServer(@NotNull String serverName, long timeoutMs) {
        return service.awaitServer(name, serverName, Duration.ofMillis(timeoutMs));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public void disconnect() {
        service.disconnect(name);
    }

    public void onEvent(@NotNull BotEventListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        service.addEventListener(listener);
    }

    public void removeEvent(@NotNull BotEventListener listener) {
        listeners.remove(listener);
        service.removeEventListener(listener);
    }

    public void clearEvents() {
        for (BotEventListener listener : listeners) {
            service.removeEventListener(listener);
        }
        listeners.clear();
    }

    @Override
    public void close() {
        disconnect();
        clearEvents();
    }
}
