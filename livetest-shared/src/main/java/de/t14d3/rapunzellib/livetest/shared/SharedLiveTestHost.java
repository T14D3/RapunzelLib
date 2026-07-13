package de.t14d3.rapunzellib.livetest.shared;

import de.t14d3.rapunzellib.commands.ConsoleCommandDispatcher;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.livetest.LiveTest;
import de.t14d3.rapunzellib.livetest.LiveTestAssertionError;
import de.t14d3.rapunzellib.livetest.LiveTestHost;
import de.t14d3.rapunzellib.livetest.LiveTestRegistry;
import de.t14d3.rapunzellib.livetest.LiveTestResult;
import de.t14d3.rapunzellib.livetest.LiveTestSkipException;
import de.t14d3.rapunzellib.livetest.LiveTestTaskManager;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared implementation of {@link LiveTestHost} that uses RapunzelLib's
 * {@link ConsoleCommandDispatcher}, {@link Scheduler}, and {@link Logger}
 * from the active {@link RapunzelContext}.
 * <p>
 * Results are reported using a standardized {@code [LIVETEST]} prefix format
 * suitable for parsing by the Gradle plugin's {@code LiveTestRunner}.
 * After all tests complete, JUnit XML and JSON report files are written
 * to the configured report directory.
 * </p>
 */
public class SharedLiveTestHost implements LiveTestHost {

    private static final String REPORT_DIR_PROPERTY = "rapunzellib.livetest.reportDir";
    private static final String DEFAULT_REPORT_DIR = "livetest-results";

    private final RapunzelContext context;
    private final LiveTestRegistry registry;
    private final Path reportDir;
    private final LiveTestTaskManager taskManager;

    /**
     * Creates a new shared live test host.
     *
     * @param context  the Rapunzel context
     * @param registry the test registry
     */
    public SharedLiveTestHost(@NotNull RapunzelContext context, @NotNull LiveTestRegistry registry) {
        this.context = Objects.requireNonNull(context, "context");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.reportDir = resolveReportDir();
        this.taskManager = new SharedLiveTestTaskManager(context.logger());
    }

    private static Path resolveReportDir() {
        String custom = System.getProperty(REPORT_DIR_PROPERTY);
        if (custom != null && !custom.isBlank()) {
            return Path.of(custom);
        }
        return Path.of(DEFAULT_REPORT_DIR);
    }

    @Override
    public @NotNull Logger logger() {
        return context.logger();
    }

    @Override
    public @NotNull Scheduler scheduler() {
        return context.scheduler();
    }

    @Override
    public @NotNull LiveTestTaskManager tasks() {
        return taskManager;
    }

    @Override
    public void reportResult(@NotNull LiveTestResult result) {
        Objects.requireNonNull(result, "result");
        String message = result.format();
        logger().info(message);
        sendMessage(message);
    }

    @Override
    public void dispatchCommand(@NotNull String command) {
        context.dispatchCommand(command);
    }

    @Override
    public void sendMessage(@NotNull String message) {
        logger().info(message);
    }

    // ── Convenience: add lifecycle commands directly on the host ──────────

    /** Registers a command to run before any test. Delegates to {@code tasks().beforeAllCommand()}. */
    public void addBeforeAllCommand(@NotNull String command) {
        taskManager.beforeAllCommand(command);
    }

    /** Registers a command to run after all tests. Delegates to {@code tasks().afterAllCommand()}. */
    public void addAfterAllCommand(@NotNull String command) {
        taskManager.afterAllCommand(command);
    }

    /** Sets a delay after afterAll tasks. Delegates to {@code tasks().afterAllDelay()}. */
    public void setAfterAllDelayMs(long delayMs) {
        taskManager.afterAllDelay(delayMs);
    }

    // ── Test execution ─────────────────────────────────────────────────────

    /**
     * Runs all tests registered in the registry.
     * <p>
     * Executes lifecycle tasks (beforeAll -> [beforeEach -> test -> afterEach] -> afterAll),
     * then writes JUnit XML and JSON report files.
     * </p>
     */
    public void runAll() {
        List<LiveTestResult> results = new ArrayList<>();

        try {
            taskManager.runBeforeAll(this);
        } catch (Exception e) {
            logger().warn("beforeAll task failed: {}", e.getMessage());
        }

        for (LiveTest test : registry.allTests()) {
            try {
                taskManager.runBeforeEach(this, test);
            } catch (Exception e) {
                logger().warn("beforeEach task failed for {}: {}", test.name(), e.getMessage());
            }

            LiveTestResult result = runTest(test, Duration.ofMillis(test.timeoutMs()));
            reportResult(result);
            results.add(result);

            try {
                taskManager.runAfterEach(this, test);
            } catch (Exception e) {
                logger().warn("afterEach task failed for {}: {}", test.name(), e.getMessage());
            }
        }

        try {
            taskManager.runAfterAll(this);
        } catch (Exception e) {
            logger().warn("afterAll task failed: {}", e.getMessage());
        }
        taskManager.delayIfNeeded();

        writeReports(results);
        sendMessage("[LIVETEST] All tests completed.");
    }

    private void writeReports(List<LiveTestResult> results) {
        try {
            new LiveTestReportWriter(reportDir, "rapunzellib-livetest")
                    .writeReports(results, Instant.now());
            logger().info("Wrote live test reports to {}", reportDir.toAbsolutePath());
        } catch (IOException e) {
            logger().warn("Failed to write live test reports to {}: {}",
                    reportDir.toAbsolutePath(), e.getMessage());
        }
    }

    /**
     * Runs a single test by name.
     *
     * @param name the test name
     * @return the test result, or a skipped result if not found
     */
    public @NotNull LiveTestResult runSingle(@NotNull String name) {
        LiveTest test = registry.find(name);
        if (test == null) {
            return LiveTestResult.skip(name, "Test not found");
        }
        LiveTestResult result = runTest(test, Duration.ofMillis(test.timeoutMs()));
        reportResult(result);
        return result;
    }
}
