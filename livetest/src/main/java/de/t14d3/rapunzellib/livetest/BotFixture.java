package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A fluent fixture wrapping a connected {@link Bot} for use in live tests.
 *
 * <p>The fixture is the recommended entry point for test code that needs
 * a bot - it brokers the connection through {@link Bot#connect}, optionally
 * waits for the bot to fully arrive on the destination server via
 * {@link Bot#awaitServer}, and guarantees cleanup via {@link AutoCloseable}.
 * The wrapped {@code Bot} is accessible through {@link #bot()} for any
 * operation the fixture does not directly expose.</p>
 *
 * <p>Typical usage in a {@link LiveTest#run()} body:</p>
 * <pre>{@code
 * try (var bot = BotFixture.connect("Tester", "lobby").join()) {
 *     bot.awaitReady();           // wait for server arrival
 *     bot.executeSync("/say hi", 2_000);
 *     Assertions.awaitChatWithin(bot.bot(), "hello", 2_000);
 * }
 * }</pre>
 *
 * <p>For one-shot scripts, {@link #with(String, String, ThrowingConsumer)} runs a
 * block with an automatically closed fixture:</p>
 * <pre>{@code
 * BotFixture.with("Tester", "lobby", bot -> {
 *     bot.awaitReady();
 *     bot.bot().executeSync("/say hi", 2_000);
 * });
 * }</pre>
 */
public final class BotFixture implements AutoCloseable {

    /**
     * Consumer that allows throwing checked exceptions.
     * Matches {@link LiveTest#run()} semantics.
     */
    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    private final @NotNull Bot bot;
    private volatile boolean closed;

    private BotFixture(@NotNull Bot bot) {
        this.bot = Objects.requireNonNull(bot, "bot");
    }

    // ── Construction ──────────────────────────────────────────────────────

    /**
     * Connects a bot to the given server. The returned future completes once
     * the transport has accepted the connection request. Call {@link #awaitReady()}
     * (or {@link Bot#awaitServer}) afterwards to wait for the bot to actually
     * arrive on the destination server.
     *
     * @param name       the bot's player name
     * @param serverName the destination server (resolved by the bot service)
     * @return a future that completes with a ready-to-use fixture
     */
    public static @NotNull CompletableFuture<BotFixture> connect(@NotNull String name, @NotNull String serverName) {
        return Bot.connect(name, serverName).thenApply(BotFixture::new);
    }

    /**
     * Synchronous helper: connects a bot, runs a block with it, then disconnects.
     * Any exception thrown by the block propagates after cleanup has run.
     *
     * @param name       the bot's player name
     * @param serverName the destination server
     * @param block      the test code to run with the connected fixture
     */
    public static void with(@NotNull String name, @NotNull String serverName, @NotNull ThrowingConsumer<BotFixture> block) {
        Objects.requireNonNull(block, "block");
        try (BotFixture fixture = connect(name, serverName).join()) {
            block.accept(fixture);
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }

    /**
     * Synchronous helper: connects a bot, awaits server arrival, runs a block,
     * then disconnects. Equivalent to wrapping {@link #with(String, String, ThrowingConsumer)}
     * with an {@code awaitReady()} call but slightly terser.
     *
     * @param name       the bot's player name
     * @param serverName the destination server
     * @param readyTimeoutMs max time to wait for server arrival
     * @param block      the test code to run with the ready fixture
     */
    public static void withReady(
            @NotNull String name,
            @NotNull String serverName,
            long readyTimeoutMs,
            @NotNull ThrowingConsumer<BotFixture> block) {
        Objects.requireNonNull(block, "block");
        try (BotFixture fixture = connect(name, serverName).join()) {
            try {
                fixture.awaitReady(readyTimeoutMs);
            } catch (Exception e) {
                throw new RuntimeException("Bot '" + name + "' did not arrive on '"
                        + serverName + "' within " + readyTimeoutMs + "ms", e);
            }
            block.accept(fixture);
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }

    // ── API ────────────────────────────────────────────────────────────────

    /**
     * Returns the wrapped bot. Direct access is intentional - the fixture is
     * a thin wrapper, and tests can use the full {@link Bot} surface.
     *
     * @return the wrapped bot
     */
    public @NotNull Bot bot() {
        ensureOpen();
        return bot;
    }

    /**
     * Waits up to {@code timeoutMs} for the bot to fully arrive on its
     * destination server (the "ready" event). Throws if the wait fails or
     * times out.
     *
     * @param timeoutMs maximum time to wait
     * @throws Exception if the wait is interrupted or times out
     */
    public void awaitReady(long timeoutMs) throws Exception {
        ensureOpen();
        bot.awaitServer(bot.server(), timeoutMs).get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /** Convenience to await a default 30s server-arrival timeout. */
    public void awaitReady() throws Exception {
        awaitReady(30_000L);
    }

    /** Runs a server command on the bot and waits for the transport to accept it. */
    public void execute(@NotNull String command, long timeoutMs) throws Exception {
        ensureOpen();
        bot.executeSync(command, timeoutMs);
    }

    /** Convenience with 2s default timeout. */
    public void execute(@NotNull String command) throws Exception {
        execute(command, 2_000L);
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            bot.disconnect();
        } catch (Exception ignored) {
            // Cleanup must never throw - close() is called from try-with-resources.
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("BotFixture for '" + bot.name() + "' is already closed");
    }
}
