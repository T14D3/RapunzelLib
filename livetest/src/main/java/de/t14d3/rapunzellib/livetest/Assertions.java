package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("unused")
public final class Assertions {
    private Assertions() {}

    public static void assertTrue(boolean condition) {
        if (!condition) throw new LiveTestAssertionError("Expected true but was false");
    }

    public static void assertTrue(boolean condition, @NotNull String message) {
        if (!condition) throw new LiveTestAssertionError(message);
    }

    public static void assertFalse(boolean condition) {
        if (condition) throw new LiveTestAssertionError("Expected false but was true");
    }

    public static void assertFalse(boolean condition, @NotNull String message) {
        if (condition) throw new LiveTestAssertionError(message);
    }

    public static void assertEquals(@Nullable Object expected, @Nullable Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new LiveTestAssertionError("Expected " + expected + " but was " + actual);
        }
    }

    public static void assertEquals(@Nullable Object expected, @Nullable Object actual, @NotNull String message) {
        if (!Objects.equals(expected, actual)) {
            throw new LiveTestAssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    public static void assertNotEquals(@Nullable Object unexpected, @Nullable Object actual) {
        if (Objects.equals(unexpected, actual)) {
            throw new LiveTestAssertionError("Did not expect " + unexpected);
        }
    }

    public static void assertNotEquals(@Nullable Object unexpected, @Nullable Object actual, @NotNull String message) {
        if (Objects.equals(unexpected, actual)) {
            throw new LiveTestAssertionError(message + " (unexpected=" + unexpected + ")");
        }
    }

    public static void assertNotNull(@Nullable Object obj) {
        if (obj == null) throw new LiveTestAssertionError("Expected non-null");
    }

    public static void assertNotNull(@Nullable Object obj, @NotNull String message) {
        if (obj == null) throw new LiveTestAssertionError(message);
    }

    public static void assertNull(@Nullable Object obj) {
        if (obj != null) throw new LiveTestAssertionError("Expected null but was " + obj);
    }

    public static void assertNull(@Nullable Object obj, @NotNull String message) {
        if (obj != null) throw new LiveTestAssertionError(message);
    }

    public static void fail(@NotNull String message) {
        throw new LiveTestAssertionError(message);
    }

    // ── Blocking async awaits ───────────────────────────────────────────────

    /**
     * Blocks until {@code future} completes and returns its value, or throws
     * {@link LiveTestAssertionError} on timeout or execution failure. This is
     * the foundation for the bot-aware assertions below.
     */
    public static <T> T await(@NotNull CompletableFuture<T> future, long timeoutMs, @NotNull String description) {
        Objects.requireNonNull(future, "future");
        Objects.requireNonNull(description, "description");
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            LiveTestContext.current().put("await", description + " (timed out after " + timeoutMs + "ms)");
            throw new LiveTestAssertionError(description + " timed out after " + timeoutMs + "ms");
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
            LiveTestContext.current().put("await", description + " (failed: " + cause.getMessage() + ")");
            throw new LiveTestAssertionError(description + " failed: " + cause.getMessage(), cause);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LiveTestContext.current().put("await", description + " (interrupted)");
            throw new LiveTestAssertionError(description + " interrupted");
        }
    }

    /**
     * Blocks until {@code bot} receives a chat message containing {@code text},
     * or fails the test with {@link LiveTestAssertionError} on timeout.
     */
    public static @NotNull String awaitChatWithin(@NotNull Bot bot, @NotNull String text, long timeoutMs) {
        Objects.requireNonNull(bot, "bot");
        Objects.requireNonNull(text, "text");
        LiveTestContext.current().put("bot", bot.name());
        return await(bot.awaitChat(text, timeoutMs), timeoutMs,
                "awaitChat('" + text + "') on bot '" + bot.name() + "'");
    }

    /**
     * Blocks until {@code bot} receives a chat message containing any of
     * {@code texts}, or fails on timeout.
     */
    public static @NotNull String awaitAnyChatWithin(@NotNull Bot bot, long timeoutMs, @NotNull String... texts) {
        Objects.requireNonNull(bot, "bot");
        Objects.requireNonNull(texts, "texts");
        LiveTestContext.current().put("bot", bot.name());
        return await(bot.awaitAnyChat(timeoutMs, texts), timeoutMs,
                "awaitAnyChat(" + String.join("/", texts) + ") on bot '" + bot.name() + "'");
    }

    // ── Inventory assertions ────────────────────────────────────────────────

    /**
     * Asserts that the bot's container snapshot eventually satisfies the
     * supplied {@link InventoryMatcher}. Polls the cached snapshot via
     * {@link Bot#awaitInventory} up to {@code timeoutMs}.
     */
    public static @NotNull BotInventory assertInventory(
            @NotNull Bot bot, int containerId, @NotNull InventoryMatcher matcher, long timeoutMs) {
        Objects.requireNonNull(bot, "bot");
        Objects.requireNonNull(matcher, "matcher");
        LiveTestContext.current().put("bot", bot.name());
        LiveTestContext.current().put("containerId", containerId);
        LiveTestContext.current().put("matcher", matcher);
        BotInventory inv = await(bot.awaitInventory(containerId, matcher, timeoutMs), timeoutMs,
                "assertInventory(containerId=" + containerId + ") on bot '" + bot.name() + "'");
        if (inv == null) {
            throw new LiveTestAssertionError(
                    "assertInventory on bot '" + bot.name() + "' completed with null snapshot");
        }
        return inv;
    }

    /** Convenience: asserts the player inventory (containerId 0) matches. */
    public static @NotNull BotInventory assertInventory(
            @NotNull Bot bot, @NotNull InventoryMatcher matcher, long timeoutMs) {
        return assertInventory(bot, 0, matcher, timeoutMs);
    }

    /**
     * Asserts that the bot's player inventory contains at least {@code amount}
     * of an item with the given registry id. Equivalent to
     * {@code assertInventory(bot, InventoryMatcher.hasAtLeast(itemId, amount), timeoutMs)}.
     */
    public static void assertHasItem(@NotNull Bot bot, int itemId, int amount, long timeoutMs) {
        assertInventory(bot, InventoryMatcher.hasAtLeast(itemId, amount), timeoutMs);
    }

    /**
     * Asserts that the bot's player inventory lacks the given item id entirely.
     */
    public static void assertLacksItem(@NotNull Bot bot, int itemId, long timeoutMs) {
        assertInventory(bot, InventoryMatcher.lacksItem(itemId), timeoutMs);
    }

    /**
     * Asserts that a specific container slot is non-empty.
     */
    public static void assertSlotNonEmpty(@NotNull Bot bot, int containerId, int slot, long timeoutMs) {
        assertInventory(bot, containerId, InventoryMatcher.slotNonEmpty(slot), timeoutMs);
    }

    /** Asserts that a specific container slot is empty. */
    public static void assertSlotEmpty(@NotNull Bot bot, int containerId, int slot, long timeoutMs) {
        assertInventory(bot, containerId, InventoryMatcher.slotEmpty(slot), timeoutMs);
    }

    // ── Entity assertions ────────────────────────────────────────────────────

    /**
     * Asserts that the bot is currently tracking at least one entity matching
     * {@code matcher}, waiting up to {@code timeoutMs} for one to appear.
     */
    public static @NotNull BotEntity assertEntity(
            @NotNull Bot bot, @NotNull EntityMatcher matcher, long timeoutMs) {
        Objects.requireNonNull(bot, "bot");
        Objects.requireNonNull(matcher, "matcher");
        LiveTestContext.current().put("bot", bot.name());
        LiveTestContext.current().put("entityMatcher", matcher);
        BotEntity entity = await(bot.awaitEntity(matcher, timeoutMs), timeoutMs,
                "assertEntity on bot '" + bot.name() + "'");
        if (entity == null || entity.isUnknown()) {
            throw new LiveTestAssertionError(
                    "assertEntity on bot '" + bot.name() + "' completed without a matching entity");
        }
        return entity;
    }

    /**
     * Asserts that the bot is currently tracking at least one entity of the
     * given type name (e.g. {@code "minecraft:zombie"} or {@code "ZOMBIE"}).
     */
    public static @NotNull BotEntity assertEntityOfType(
            @NotNull Bot bot, @NotNull String typeName, long timeoutMs) {
        Objects.requireNonNull(typeName, "typeName");
        return assertEntity(bot, EntityMatcher.ofType(typeName), timeoutMs);
    }

    /**
     * Asserts that the bot is currently <em>not</em> tracking any entity
     * matching {@code matcher}. Uses a single snapshot query (no waiting).
     */
    public static void assertNoEntity(@NotNull Bot bot, @NotNull EntityMatcher matcher, long timeoutMs) {
        Objects.requireNonNull(bot, "bot");
        Objects.requireNonNull(matcher, "matcher");
        LiveTestContext.current().put("bot", bot.name());
        LiveTestContext.current().put("entityMatcher", matcher);
        List<BotEntity> tracked = await(bot.queryMatchingEntities(matcher, timeoutMs), timeoutMs,
                "assertNoEntity (query) on bot '" + bot.name() + "'");
        if (tracked != null && !tracked.isEmpty()) {
            throw new LiveTestAssertionError(
                    "assertNoEntity on bot '" + bot.name() + "' found " + tracked.size()
                            + " matching entities (first=" + tracked.get(0) + ")");
        }
    }

    // ── Abilities / player state assertions ──────────────────────────────────

    /**
     * Asserts that the bot's abilities snapshot has been received and returns
     * it. Queries up to {@code timeoutMs}.
     */
    public static @NotNull BotAbilities assertAbilities(@NotNull Bot bot, long timeoutMs) {
        Objects.requireNonNull(bot, "bot");
        LiveTestContext.current().put("bot", bot.name());
        BotAbilities a = await(bot.queryAbilities(timeoutMs), timeoutMs,
                "assertAbilities on bot '" + bot.name() + "'");
        if (a == null) {
            throw new LiveTestAssertionError(
                    "assertAbilities on bot '" + bot.name() + "' completed with null");
        }
        LiveTestContext.current().put("abilities", a);
        return a;
    }

    /** Asserts that the bot is currently in creative mode (per the server's abilities packet). */
    public static void assertCreative(@NotNull Bot bot, long timeoutMs) {
        Objects.requireNonNull(bot, "bot");
        LiveTestContext.current().put("bot", bot.name());
        long deadline = System.currentTimeMillis() + timeoutMs;
        Exception lastEx = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                BotAbilities a = bot.queryAbilities(
                        Math.max(500, deadline - System.currentTimeMillis())).get();
                if (a != null && a.creative()) {
                    LiveTestContext.current().put("abilities", a);
                    return;
                }
                if (a != null) {
                    // creative=false - the game-mode change may still be
                    // propagating through the BotClient -> BotTcpServer ->
                    // RpcBotService pipeline.  Wait a bit and retry.
                    try { Thread.sleep(200); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new LiveTestAssertionError("assertCreative interrupted");
                    }
                    continue;
                }
            } catch (Exception e) {
                lastEx = e;
                // Transient failure - retry unless we've run out of time.
                try { Thread.sleep(200); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new LiveTestAssertionError("assertCreative interrupted");
                }
            }
        }
        // Timeout - report the final state for diagnostics.
        try {
            BotAbilities a = bot.queryAbilities(1_000L).get();
            if (a != null) {
                throw new LiveTestAssertionError(
                        "Expected bot '" + bot.name() + "' to be in creative mode but abilities=" + a);
            }
        } catch (Exception e2) {
            // fall through to the last-exception path
        }
        if (lastEx != null) {
            throw new LiveTestAssertionError(
                    "assertCreative on bot '" + bot.name() + "' failed: " + lastEx.getMessage(), lastEx);
        }
        throw new LiveTestAssertionError(
                "assertCreative on bot '" + bot.name() + "' timed out after " + timeoutMs + "ms");
    }

    /** Asserts that the bot is currently flying (per the server's abilities packet). */
    public static void assertFlying(@NotNull Bot bot, long timeoutMs) {
        BotAbilities a = assertAbilities(bot, timeoutMs);
        if (!a.flying()) {
            throw new LiveTestAssertionError(
                    "Expected bot '" + bot.name() + "' to be flying but abilities=" + a);
        }
    }

    /** Asserts that the bot is allowed to fly. */
    public static void assertCanFly(@NotNull Bot bot, long timeoutMs) {
        BotAbilities a = assertAbilities(bot, timeoutMs);
        if (!a.canFly()) {
            throw new LiveTestAssertionError(
                    "Expected bot '" + bot.name() + "' to be able to fly but abilities=" + a);
        }
    }

    // ── Tab-completion assertions ─────────────────────────────────────────────

    /**
     * Asserts that the server's tab-completion response for {@code text}
     * contains the expected match string. Blocks up to {@code timeoutMs}.
     */
    public static void assertTabCompleteContains(
            @NotNull Bot bot, @NotNull String text, @NotNull String expectedMatch, long timeoutMs) {
        Objects.requireNonNull(bot, "bot");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(expectedMatch, "expectedMatch");
        LiveTestContext.current().put("bot", bot.name());
        LiveTestContext.current().put("tabCompleteInput", text);
        List<BotSuggestion> suggestions = await(bot.queryTabCompleteAsync(text, timeoutMs), timeoutMs,
                "assertTabCompleteContains('" + text + "') on bot '" + bot.name() + "'");
        if (suggestions == null || suggestions.isEmpty()) {
            throw new LiveTestAssertionError(
                    "Expected tab-completion of '" + text + "' to contain '" + expectedMatch
                            + "' but no suggestions were returned");
        }
        for (BotSuggestion s : suggestions) {
            if (expectedMatch.equals(s.match())) return;
        }
        LiveTestContext.current().put("suggestions", suggestions);
        throw new LiveTestAssertionError(
                "Expected tab-completion of '" + text + "' to contain '" + expectedMatch
                        + "' but got " + suggestions);
    }

    /** Asserts that the server's tab-completion response for {@code text} is empty. */
    public static void assertTabCompleteEmpty(@NotNull Bot bot, @NotNull String text, long timeoutMs) {
        Objects.requireNonNull(bot, "bot");
        Objects.requireNonNull(text, "text");
        LiveTestContext.current().put("bot", bot.name());
        LiveTestContext.current().put("tabCompleteInput", text);
        List<BotSuggestion> suggestions = await(bot.queryTabCompleteAsync(text, timeoutMs), timeoutMs,
                "assertTabCompleteEmpty('" + text + "') on bot '" + bot.name() + "'");
        if (suggestions != null && !suggestions.isEmpty()) {
            LiveTestContext.current().put("suggestions", suggestions);
            throw new LiveTestAssertionError(
                    "Expected tab-completion of '" + text + "' to be empty but got " + suggestions);
        }
    }

    // ── Command execution ────────────────────────────────────────────────────

    // ── Block state assertions ─────────────────────────────────────────────

    /**
     * Asserts that the bot observes a block change event at the given position
     * with the expected block state id, waiting up to {@code timeoutMs}.
     */
    public static void assertBlockState(@NotNull Bot bot, int x, int y, int z, int expectedStateId, long timeoutMs, String message) throws Exception {
        try {
            bot.awaitBlock(x, y, z, expectedStateId, timeoutMs).get();
        } catch (Exception e) {
            throw new AssertionError(message != null ? message : "Expected block at (" + x + "," + y + "," + z + ") to have stateId " + expectedStateId + " within " + timeoutMs + "ms", e);
        }
    }

    /**
     * Asserts that the bot observes an explosion with at least the given minimum
     * radius, waiting up to {@code timeoutMs}.
     */
    public static void assertExplosion(@NotNull Bot bot, float minRadius, long timeoutMs, String message) throws Exception {
        try {
            bot.awaitExplosion(minRadius, timeoutMs).get();
        } catch (Exception e) {
            throw new AssertionError(message != null ? message : "Expected explosion with radius >= " + minRadius + " within " + timeoutMs + "ms", e);
        }
    }

    // ── Command execution ────────────────────────────────────────────────────

    /**
     * Sends a server command via the bot, blocking until the transport has
     * accepted it. Surfaces transport failures as {@link LiveTestAssertionError}.
     * NOTE: This only confirms the command was sent - it does not assert the
     * command's server-side effect, because bot clients don't observe server
     * command results directly. Pair with {@link #awaitChatWithin} to verify
     * the effect (e.g. command output appears in chat) when needed.
     */
    public static void executeCommand(@NotNull Bot bot, @NotNull String command, long timeoutMs) {
        Objects.requireNonNull(bot, "bot");
        Objects.requireNonNull(command, "command");
        LiveTestContext.current().put("bot", bot.name());
        LiveTestContext.current().put("command", command);
        try {
            bot.executeSync(command, timeoutMs);
        } catch (Exception e) {
            throw new LiveTestAssertionError(
                    "executeCommand('" + command + "') on bot '" + bot.name() + "' failed: " + e.getMessage(), e);
        }
    }

    /** Convenience: 2 second default timeout. */
    public static void executeCommand(@NotNull Bot bot, @NotNull String command) {
        executeCommand(bot, command, 2_000L);
    }

    /**
     * Sends a console command via the {@link LiveTestHost} (server-side). Use
     * for setup/teardown commands that need to run with elevated privileges
     * rather than as the bot.
     */
    public static void runConsoleCommand(@NotNull LiveTestHost host, @NotNull String command) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(command, "command");
        LiveTestContext.current().put("consoleCommand", command);
        try {
            host.dispatchCommand(command);
        } catch (Exception e) {
            throw new LiveTestAssertionError(
                    "runConsoleCommand('" + command + "') failed: " + e.getMessage(), e);
        }
    }
}

