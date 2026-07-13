package de.t14d3.rapunzellib.livetest;

import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Platform abstraction for hosting and executing live tests.
 * <p>
 * Implementations provide the server-side infrastructure needed to run tests:
 * scheduling, command dispatching, result reporting, and logging.
 * </p>
 * <p>
 * A {@code LiveTestHost} is registered as a service in the
 * {@link de.t14d3.rapunzellib.context.RapunzelContext}'s
 * {@link de.t14d3.rapunzellib.context.ServiceRegistry} by the
 * {@link LiveTestFeatureInstaller} for the active platform.
 * </p>
 */
public interface LiveTestHost {

    /**
     * Returns the logger for this host.
     *
     * @return the logger
     */
    @NotNull Logger logger();

    /**
     * Returns the scheduler for this host.
     *
     * @return the scheduler
     */
    @NotNull Scheduler scheduler();

    /**
     * Reports the result of a single live test.
     * <p>
     * Implementations should format and output the result in a platform-appropriate
     * way (e.g., console log with {@code [LIVETEST]} prefix, in-game chat, etc.).
     * </p>
     *
     * @param result the test result to report
     */
    void reportResult(@NotNull LiveTestResult result);

    /**
     * Dispatches a server command as the console.
     * <p>
     * The command should be executed on the server's main thread if the platform requires it.
     * </p>
     *
     * @param command the command to execute (e.g., {@code "/gamemode creative Tester"})
     */
    void dispatchCommand(@NotNull String command);

    /**
     * Sends a message to the server console or appropriate output.
     *
     * @param message the message to send
     */
    void sendMessage(@NotNull String message);

    // ── Lifecycle task manager ───────────────────────────────────────────

    /**
     * Returns the {@link LiveTestTaskManager} for registering beforeAll/afterAll
     * and beforeEach/afterEach lifecycle tasks.
     * <p>
     * Test plugin authors register tasks in their {@code onEnable()}:
     * <pre>{@code
     * LiveTestHost host = LiveTestFeatures.host(context);
     * host.tasks().beforeAllCommand("spark profiler start --interval 1");
     * host.tasks().afterAllCommand("spark profiler stop");
     * host.tasks().afterAllDelay(10_000L);
     * }</pre>
     * </p>
     */
    @NotNull LiveTestTaskManager tasks();

    /**
     * Runs a single test with timeout handling.
     * <p>
     * Default implementation runs the test synchronously with the configured timeout.
     * Platform implementations may override to provide async execution with proper
     * timeout enforcement.
     * </p>
     *
     * @param test    the test to run
     * @param timeout the maximum duration to wait for the test to complete
     * @return the test result
     */
    default @NotNull LiveTestResult runTest(@NotNull LiveTest test, @NotNull Duration timeout) {
        long start = System.currentTimeMillis();
        try {
            test.setupCommands();
            test.run();
            long elapsed = System.currentTimeMillis() - start;
            return LiveTestResult.pass(test.name(), elapsed);
        } catch (LiveTestSkipException e) {
            return LiveTestResult.skip(test.name(), e.getMessage());
        } catch (LiveTestAssertionError e) {
            long elapsed = System.currentTimeMillis() - start;
            return LiveTestResult.fail(test.name(), elapsed, e.getMessage());
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return LiveTestResult.error(test.name(), elapsed, e.getMessage());
        }
    }
}
