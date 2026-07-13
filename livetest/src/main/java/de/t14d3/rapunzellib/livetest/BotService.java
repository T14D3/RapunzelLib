package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing Minecraft bot clients used in live tests.
 * <p>
 * Implementations provide the platform-specific mechanism for connecting,
 * controlling, and querying automated bot clients. Bots are typically
 * separate Minecraft client processes used to simulate player actions
 * during testing.
 * </p>
 * <p>
 * A {@code BotService} is registered as a service in the
 * {@link de.t14d3.rapunzellib.context.RapunzelContext}'s
 * {@link de.t14d3.rapunzellib.context.ServiceRegistry}.
 * </p>
 */
public interface BotService {

    /**
     * Connects a bot to the specified server.
     *
     * @param name       the bot's player name
     * @param serverName the server address or name to connect to
     * @return a future that completes with a connected bot handle
     */
    @NotNull CompletableFuture<Bot> connect(@NotNull String name, @NotNull String serverName);

    /**
     * Disconnects a bot.
     *
     * @param name the bot's player name
     */
    void disconnect(@NotNull String name);

    /**
     * Executes a command as the bot.
     *
     * @param name    the bot's player name
     * @param command the command to execute
     */
    void execute(@NotNull String name, @NotNull String command);

    /**
     * Waits for a chat message from the bot containing the given text.
     *
     * @param name    the bot's player name
     * @param text    the text to look for in the chat message
     * @param timeout the maximum time to wait
     * @return a future that completes with the matched chat message
     */
    @NotNull CompletableFuture<String> awaitChat(@NotNull String name, @NotNull String text, @NotNull Duration timeout);

    /**
     * Waits for a chat message from the bot matching the given regex pattern.
     *
     * @param name    the bot's player name
     * @param pattern the regex pattern to match
     * @param timeout the maximum time to wait
     * @return a future that completes with the matched chat message
     */
    @NotNull CompletableFuture<String> awaitChat(@NotNull String name, @NotNull java.util.regex.Pattern pattern, @NotNull Duration timeout);

    /**
     * Waits for a chat message from the bot containing any of the given texts.
     *
     * @param name    the bot's player name
     * @param timeout the maximum time to wait
     * @param texts   the texts to look for
     * @return a future that completes with the matched chat message
     */
    @NotNull CompletableFuture<String> awaitAnyChat(@NotNull String name, @NotNull Duration timeout, @NotNull String... texts);

    /**
     * Queries the bot's current position.
     *
     * @param name    the bot's player name
     * @param timeout the maximum time to wait
     * @return a future that completes with the bot's position
     */
    @NotNull CompletableFuture<Bot.Position> queryPosition(@NotNull String name, @NotNull Duration timeout);

    /**
     * Queries the bot's health, food, and saturation.
     *
     * @param name    the bot's player name
     * @param timeout the maximum time to wait
     * @return a future that completes with the bot's health data
     */
    @NotNull CompletableFuture<Bot.Health> queryHealth(@NotNull String name, @NotNull Duration timeout);

    /**
     * Queries the bot's currently held item.
     *
     * @param name    the bot's player name
     * @param timeout the maximum time to wait
     * @return a future that completes with the bot's held item data
     */
    @NotNull CompletableFuture<Bot.HeldItem> queryHeldItem(@NotNull String name, @NotNull Duration timeout);

    /**
     * Queries the bot's current game mode.
     *
     * @param name    the bot's player name
     * @param timeout the maximum time to wait
     * @return a future that completes with the game mode string (e.g., "creative", "survival")
     */
    @NotNull CompletableFuture<String> queryGameMode(@NotNull String name, @NotNull Duration timeout);

    /**
     * Queries the bot's currently open container ID.
     *
     * @param name    the bot's player name
     * @param timeout the maximum time to wait
     * @return a future that completes with the container ID, or -1 if no container is open
     */
    @NotNull CompletableFuture<Integer> queryOpenContainer(@NotNull String name, @NotNull Duration timeout);

    /**
     * Queries entity IDs tracked by the bot matching the given type name.
     *
     * @param name     the bot's player name
     * @param typeName the entity type name (e.g. "ZOMBIE", "SHEEP")
     * @param timeout  the maximum time to wait
     * @return a future that completes with an array of matching entity IDs
     */
    @NotNull CompletableFuture<int[]> queryEntities(@NotNull String name, @NotNull String typeName, @NotNull Duration timeout);

    /**
     * Requests the bot to dig a block at the specified position.
     *
     * @param name      the bot's player name
     * @param x         the block's x coordinate
     * @param y         the block's y coordinate
     * @param z         the block's z coordinate
     * @param direction the direction to dig from
     */
    void digBlock(@NotNull String name, int x, int y, int z, @NotNull Bot.Direction direction);

    /**
     * Requests the bot to use an item on a block at the specified position.
     *
     * @param name      the bot's player name
     * @param x         the block's x coordinate
     * @param y         the block's y coordinate
     * @param z         the block's z coordinate
     * @param hand      the hand to use
     * @param direction the direction to interact from
     */
    void useItemOn(@NotNull String name, int x, int y, int z, @NotNull Bot.Hand hand, @NotNull Bot.Direction direction);

    /**
     * Requests the bot to move to a specific block position.
     *
     * @param name the bot's player name
     * @param x    the target x coordinate
     * @param y    the target y coordinate
     * @param z    the target z coordinate
     */
    void moveTo(@NotNull String name, int x, int y, int z);

    /**
     * Requests the bot to attack an entity.
     *
     * @param name     the bot's player name
     * @param entityId the entity ID to attack
     */
    void attackEntity(@NotNull String name, int entityId);

    /**
     * Requests the bot to interact (right-click) with an entity.
     *
     * @param name     the bot's player name
     * @param entityId the entity ID to interact with
     * @param hand     the hand to use (0 = MAIN_HAND, 1 = OFF_HAND)
     */
    void interactEntity(@NotNull String name, int entityId, int hand);

    /**
     * Requests the bot to swing its hand (animation only).
     *
     * @param name the bot's player name
     * @param hand the hand to swing (0 = MAIN_HAND, 1 = OFF_HAND)
     */
    void swingHand(@NotNull String name, int hand);

    /**
     * Sets the bot's held item slot on the hotbar.
     *
     * @param name the bot's player name
     * @param slot the hotbar slot (0-8)
     */
    void setHeldItemSlot(@NotNull String name, int slot);

    /**
     * Waits for the bot to arrive at a position within the given radius.
     *
     * @param name    the bot's player name
     * @param x       the target x coordinate
     * @param y       the target y coordinate
     * @param z       the target z coordinate
     * @param radius  the acceptable radius from the target
     * @param timeout the maximum time to wait
     * @return a future that completes with the bot's position when it arrives
     */
    @NotNull CompletableFuture<Bot.Position> awaitPosition(@NotNull String name, int x, int y, int z, double radius, @NotNull Duration timeout);

    /**
     * Waits for the bot to be on a specific server (for proxy networks).
     *
     * @param name       the bot's player name
     * @param serverName the target server name
     * @param timeout    the maximum time to wait
     * @return a future that completes when the bot is on the server
     */
    @NotNull CompletableFuture<Void> awaitServer(@NotNull String name, @NotNull String serverName, @NotNull Duration timeout);

    /**
     * Adds a listener for bot events.
     *
     * @param listener the listener to add
     */
    void addEventListener(@NotNull BotEventListener listener);

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    void removeEventListener(@NotNull BotEventListener listener);

    /**
     * Injects an external bot event into this service.
     * <p>
     * Called by the framework when the console receives a bot callback
     * (e.g., from the DevRunner via {@code /botcallback}). Implementations
     * should add the event to their processing queue and notify any
     * registered {@link BotEventListener listeners}.
     * </p>
     *
     * @param type    the event type (e.g., "READY", "CHAT", "POSITION", "HEALTH", "ERROR")
     * @param botName the bot name
     * @param message the event payload
     */
    default void injectEvent(@NotNull String type, @NotNull String botName, @NotNull String message) {
        // default: no-op
    }
}