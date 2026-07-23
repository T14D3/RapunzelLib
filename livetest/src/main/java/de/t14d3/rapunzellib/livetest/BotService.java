package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

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
     * @return a future that completes when the command has been sent
     */
    @NotNull CompletableFuture<Void> execute(@NotNull String name, @NotNull String command);

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

    // ── Inventory read ──────────────────────────────────────────────────────

    /**
     * Queries the bot's inventory snapshot for a given container.
     *
     * @param name        the bot's player name
     * @param containerId the protocol container id ({@code 0} for player inventory,
     *                    or the id previously returned by {@link #queryOpenContainer})
     * @param timeout     the maximum time to wait
     * @return a future that completes with the inventory snapshot, or
     *         completes exceptionally if the bot is unknown or the query times out
     */
    @NotNull CompletableFuture<BotInventory> queryInventory(@NotNull String name, int containerId, @NotNull Duration timeout);

    /**
     * Waits until the bot's container snapshot matches the given matcher.
     * <p>
     * Implementations should start by issuing a query for the current snapshot
     * (which lets the matcher fire on the present state without waiting for an
     * inventory event) and then subscribe to subsequent inventory updates until
     * the matcher passes or the timeout expires.
     * </p>
     *
     * @param name        the bot's player name
     * @param containerId the container id to watch
     * @param matcher     test applied to each snapshot
     * @param timeout     the maximum time to wait
     * @return a future that completes with the matching snapshot
     */
    @NotNull CompletableFuture<BotInventory> awaitInventory(@NotNull String name,
                                                             int containerId,
                                                             @NotNull InventoryMatcher matcher,
                                                             @NotNull Duration timeout);

    // ── Inventory write ─────────────────────────────────────────────────────

    /**
     * Performs a click in a container slot.
     *
     * @param name        the bot's player name
     * @param containerId the container id (use {@code 0} for the player inventory)
     * @param slot        the protocol slot index (use {@code -999} for "outside the window")
     * @param button      the mouse button (0 = left, 1 = right)
     * @param clickType   the kind of click
     * @return a future that completes when the click packet has been sent,
     *         or completes exceptionally if the bot is unknown
     */
    @NotNull CompletableFuture<Void> clickSlot(@NotNull String name,
                                              int containerId,
                                              int slot,
                                              int button,
                                              @NotNull BotInventory.ClickType clickType);

    /**
     * Closes the bot's currently open container.
     *
     * @param name        the bot's player name
     * @param containerId the container id to close (use {@code 0} for the player inventory
     *                    to drop back to the default view, or the open container's id)
     * @return a future that completes when the close packet has been sent
     */
    @NotNull CompletableFuture<Void> closeContainer(@NotNull String name, int containerId);

    /**
     * Drops the bot's currently held stack (creative or survival).
     *
     * @param name     the bot's player name
     * @param dropAll  if {@code true} drop the whole held stack; otherwise drop one item
     * @return a future that completes when the drop packet has been sent
     */
    @NotNull CompletableFuture<Void> dropHeldItem(@NotNull String name, boolean dropAll);

    /**
     * Places an item into a slot using the creative-mode slot packet.
     * <p>
     * Has no effect if the bot is not in creative mode.
     * </p>
     *
     * @param name  the bot's player name
     * @param slot  the destination slot (protocol slot index)
     * @param stack the item stack to place (use {@link BotItemStack#EMPTY} to clear)
     * @return a future that completes when the packet has been sent
     */
    @NotNull CompletableFuture<Void> setCreativeSlot(@NotNull String name,
                                                    int slot,
                                                    @NotNull BotItemStack stack);

    // ── Await primitive ────────────────────────────────────────────────────

    /**
     * Waits for a bot event matching the given predicate.
     * <p>
     * This is the lowest-level await primitive; the higher-level
     * {@code awaitChat}/{@code awaitInventory} helpers are built on top of it.
     * Implementations are expected to register a transient listener, complete
     * the returned future the first time the predicate returns {@code true},
     * and remove the listener on completion/timeout.
     * </p>
     *
     * @param name      the bot's player name (may be matched against {@link BotEventListener.BotEvent#botName()})
     * @param predicate matched against each event for the named bot
     * @param timeout   the maximum time to wait
     * @return a future that completes with the matched event, or completes
     *         exceptionally with a timeout
     */
    @NotNull CompletableFuture<BotEventListener.BotEvent> awaitEvent(
            @NotNull String name,
            @NotNull Predicate<BotEventListener.BotEvent> predicate,
            @NotNull Duration timeout);

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

    // ── Tab-completion (B) ────────────────────────────────────────────────

    /**
     * Requests tab-completion suggestions for the given partial text.
     *
     * @param name    the bot's player name
     * @param text    the partial command/chat text (typically starting with {@code /})
     * @param timeout the maximum time to wait
     * @return a future that completes with the list of suggestions; never {@code null}
     */
    @NotNull CompletableFuture<java.util.List<BotSuggestion>> queryTabComplete(
            @NotNull String name, @NotNull String text, @NotNull Duration timeout);

    // ── Entity snapshots (B/C) ─────────────────────────────────────────────

    /**
     * Queries the snapshot of every tracked entity that matches the given
     * entity matcher, evaluated on the bot's most recent local percept.
     *
     * @param name    the bot's player name
     * @param matcher predicate applied to each tracked entity
     * @param timeout the maximum time to wait
     * @return a future that completes with the matching entity snapshots (may be empty)
     */
    @NotNull CompletableFuture<java.util.List<BotEntity>> queryMatchingEntities(
            @NotNull String name, @NotNull EntityMatcher matcher, @NotNull Duration timeout);

    /**
     * Queries a single tracked entity by its entity id. Returns
     * {@link BotEntity#EMPTY} if the bot has never heard of that id.
     */
    @NotNull CompletableFuture<BotEntity> queryEntity(@NotNull String name, int entityId, @NotNull Duration timeout);

    /**
     * Awaits at least one entity matching the given matcher.
     *
     * @param name    the bot's player name
     * @param matcher predicate applied to each tracked entity
     * @param timeout the maximum time to wait
     * @return a future that completes with the first matching entity
     */
    @NotNull CompletableFuture<BotEntity> awaitEntity(
            @NotNull String name, @NotNull EntityMatcher matcher, @NotNull Duration timeout);

    // ── Player self-state (C) ─────────────────────────────────────────────

    /**
     * Queries the bot's current ability flags.
     */
    @NotNull CompletableFuture<BotAbilities> queryAbilities(@NotNull String name, @NotNull Duration timeout);

    /**
     * Sends a respawn request (used in the death screen -> {@code ClientCommand.PERFORM_RESPAWN}).
     * @return a future that completes when the request has been dispatched
     */
    @NotNull CompletableFuture<Void> respawn(@NotNull String name);

    /**
     * Sets the player's input flags (forward/back/left/right/jump/shift/sprint) for one tick.
     * Useful for one-shot state changes like toggling sneak or sprint.
     */
    @NotNull CompletableFuture<Void> sendInput(@NotNull String name,
                                               boolean forward, boolean backward,
                                               boolean left, boolean right,
                                               boolean jump,
                                               boolean sneak,
                                               boolean sprint);

    /**
     * Convenience for {@link #sendInput} that toggles just the sneak state.
     * @param enable {@code true} for start sneaking; {@code false} for stop sneaking
     */
    default @NotNull CompletableFuture<Void> setSneaking(@NotNull String name, boolean enable) {
        return sendInput(name, false, false, false, false, false, enable, false);
    }

    /**
     * Convenience for {@link #sendInput} that toggles just the sprint state.
     */
    default @NotNull CompletableFuture<Void> setSprinting(@NotNull String name, boolean enable) {
        return sendInput(name, false, false, false, false, false, false, enable);
    }

    /**
     * Toggles flying (only meaningful if the bot {@link BotAbilities#canFly() can fly}).
     * @param enable {@code true} to take off; {@code false} to land
     */
    @NotNull CompletableFuture<Void> setFlying(@NotNull String name, boolean enable);

    /**
     * Uses the currently held item (right-click in air = {@code ServerboundUseItemPacket}).
     */
    @NotNull CompletableFuture<Void> useItem(@NotNull String name, @NotNull Bot.Hand hand);

    // ── Block-change tracking ──────────────────────────────────────────────

    /**
     * Awaits a block change event at the given position with the expected
     * block state id. Uses the accumulated block snapshots from the bot's
     * client-side packet listener.
     *
     * @param botName         the bot's player name
     * @param x               block x coordinate
     * @param y               block y coordinate
     * @param z               block z coordinate
     * @param expectedStateId the expected raw block state id (protocol-level)
     * @param timeoutMs       max wait time in milliseconds
     * @return a future that completes when the matching block snapshot is observed
     */
    @NotNull CompletableFuture<Void> awaitBlock(@NotNull String botName, int x, int y, int z, int expectedStateId, long timeoutMs);

    /**
     * Like {@link #awaitBlock(String, int, int, int, int, long)} but operates on
     * the first bot known to the service.
     */
    @NotNull CompletableFuture<Void> awaitBlock(int x, int y, int z, int expectedStateId, long timeoutMs);

    /**
     * Queries the accumulated block snapshots for a named bot.
     * Sends a {@code query_blocks} RPC request to the DevRunner.
     */
    @NotNull CompletableFuture<List<BlockSnapshot>> queryBlocks(@NotNull String botName);

    /**
     * Clears the accumulated block snapshots for a named bot (both client-side
     * and local cache). Fire-and-forget.
     */
    void clearBlocks(@NotNull String botName);

    // ── Explosion tracking ──────────────────────────────────────────────

    /**
     * Awaits an explosion event for the named bot with at least the given minimum
     * radius. Uses the latest explosion snapshot from the bot's client-side
     * packet listener.
     *
     * @param botName   the bot's player name
     * @param minRadius minimum explosion radius to wait for
     * @param timeoutMs max wait time in milliseconds
     * @return a future that completes when a matching explosion is observed
     */
    @NotNull CompletableFuture<Void> awaitExplosion(@NotNull String botName, float minRadius, long timeoutMs);

    /**
     * Like {@link #awaitExplosion(String, float, long)} but operates on the
     * first bot known to the service.
     */
    @NotNull CompletableFuture<Void> awaitExplosion(float minRadius, long timeoutMs);

    /**
     * Queries the latest explosion snapshot for a named bot. Returns {@code null}
     * if no explosion has been observed yet.
     */
    @NotNull CompletableFuture<ExplosionSnapshot> queryExplosion(@NotNull String botName);
}
