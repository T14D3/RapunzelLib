package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight, thread-local diagnostic context attached to live test failures.
 *
 * <p>Bot-aware assertions called within a {@link LiveTest#run()} body
 * populate this context with the test's last-known bot state - the bot's
 * name, the matcher or container that was inspected, the last chat message
 * observed, and so on. When the test fails, {@link LiveTestHost#runTest}
 * captures the context and embeds it in the {@link LiveTestResult} so test
 * reports show the conditions surrounding the failure.</p>
 *
 * <p>The context is scoped to the running thread; it is reset between tests
 * by the host. Test authors rarely interact with this class directly -
 * assertion helpers in {@link Assertions} populate entries automatically.</p>
 */
public final class LiveTestContext {

    private static final ThreadLocal<LiveTestContext> CURRENT = ThreadLocal.withInitial(LiveTestContext::new);

    private final StringBuilder buffer = new StringBuilder();

    private LiveTestContext() {}

    /** Returns the context bound to the current thread. */
    public static @NotNull LiveTestContext current() {
        return CURRENT.get();
    }

    /** Clears the current thread's context (called by the host between tests). */
    public static void reset() {
        LiveTestContext ctx = CURRENT.get();
        ctx.buffer.setLength(0);
    }

    /**
     * Appends a labelled line to the context. {@code label} is rendered as
     * {@code "label: value"} on its own line. Null values are skipped.
     *
     * @param label the diagnostic label
     * @param value the diagnostic value (skipped if null)
     */
    public void put(@NotNull String label, @Nullable Object value) {
        if (value == null) return;
        if (buffer.length() > 0) buffer.append('\n');
        buffer.append(label).append(": ").append(value);
    }

    /** Returns the rendered context, or {@code null} if empty. */
    public @Nullable String render() {
        return buffer.length() > 0 ? buffer.toString() : null;
    }
}
