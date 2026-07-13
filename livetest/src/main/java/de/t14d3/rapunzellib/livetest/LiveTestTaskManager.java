package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

/**
 * Manages lifecycle tasks for live test execution.
 *
 * <p>Test plugin authors can register tasks from their {@code onEnable()}:
 * <pre>{@code
 * LiveTestHost host = LiveTestFeatures.host(context);
 * host.tasks().beforeAll(h -> h.dispatchCommand("spark profiler start --interval 1"));
 * host.tasks().afterAll(h -> h.dispatchCommand("spark profiler stop"));
 * host.tasks().afterAllDelay(10_000L);
 * }</pre>
 * </p>
 *
 * <p>For simple server-command use cases, convenience methods are provided:
 * <pre>{@code
 * host.tasks().beforeAllCommand("spark profiler start --interval 1");
 * host.tasks().afterAllCommand("spark profiler stop");
 * }</pre>
 * </p>
 */
public interface LiveTestTaskManager {

    /** Register a task to run once before any test. */
    void beforeAll(@NotNull LiveTestTask task);

    /** Register a task to run once after all tests complete. */
    void afterAll(@NotNull LiveTestTask task);

    /** Register a task to run before each individual test. */
    void beforeEach(@NotNull LiveTestTask task);

    /** Register a task to run after each individual test. */
    void afterEach(@NotNull LiveTestTask task);

    /**
     * Convenience: register a server command to run once before any test.
     * Equivalent to {@code beforeAll(h -> h.dispatchCommand(command))}.
     */
    default void beforeAllCommand(@NotNull String command) {
        beforeAll(host -> host.dispatchCommand(command));
    }

    /**
     * Convenience: register a server command to run once after all tests.
     * Equivalent to {@code afterAll(h -> h.dispatchCommand(command))}.
     */
    default void afterAllCommand(@NotNull String command) {
        afterAll(host -> host.dispatchCommand(command));
    }

    /**
     * Sets a delay in milliseconds to wait after {@link #afterAll} tasks
     * have completed before signalling test completion. Useful for profiling
     * tools that need time to flush data.
     */
    void afterAllDelay(long millis);

    // ── Internal execution methods (called by the host) ──────────────────

    /** @hidden */
    void runBeforeAll(@NotNull LiveTestHost host) throws Exception;

    /** @hidden */
    void runAfterAll(@NotNull LiveTestHost host) throws Exception;

    /** @hidden */
    void runBeforeEach(@NotNull LiveTestHost host, @NotNull LiveTest test) throws Exception;

    /** @hidden */
    void runAfterEach(@NotNull LiveTestHost host, @NotNull LiveTest test) throws Exception;

    /** @hidden */
    void delayIfNeeded();
}
