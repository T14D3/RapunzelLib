package de.t14d3.rapunzellib.livetest.shared;

import de.t14d3.rapunzellib.livetest.LiveTest;
import de.t14d3.rapunzellib.livetest.LiveTestHost;
import de.t14d3.rapunzellib.livetest.LiveTestTask;
import de.t14d3.rapunzellib.livetest.LiveTestTaskManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared implementation of {@link LiveTestTaskManager}.
 *
 * <p>Accumulates lifecycle tasks and runs them at the appropriate points
 * during test execution. Thread-safe for registration; execution happens
 * on the caller's thread.</p>
 */
public final class SharedLiveTestTaskManager implements LiveTestTaskManager {

    private final Logger logger;
    private final List<LiveTestTask> beforeAllTasks = new ArrayList<>();
    private final List<LiveTestTask> afterAllTasks = new ArrayList<>();
    private final List<LiveTestTask> beforeEachTasks = new ArrayList<>();
    private final List<LiveTestTask> afterEachTasks = new ArrayList<>();
    private volatile long afterAllDelayMs;

    public SharedLiveTestTaskManager(@NotNull Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void beforeAll(@NotNull LiveTestTask task) {
        beforeAllTasks.add(Objects.requireNonNull(task, "task"));
    }

    @Override
    public void afterAll(@NotNull LiveTestTask task) {
        afterAllTasks.add(Objects.requireNonNull(task, "task"));
    }

    @Override
    public void beforeEach(@NotNull LiveTestTask task) {
        beforeEachTasks.add(Objects.requireNonNull(task, "task"));
    }

    @Override
    public void afterEach(@NotNull LiveTestTask task) {
        afterEachTasks.add(Objects.requireNonNull(task, "task"));
    }

    @Override
    public void afterAllDelay(long millis) {
        this.afterAllDelayMs = Math.max(0, millis);
    }

    // ── Execution ─────────────────────────────────────────────────────────

    @Override
    public void runBeforeAll(@NotNull LiveTestHost host) throws Exception {
        for (LiveTestTask task : beforeAllTasks) {
            logger.info("[LIVETEST] Running beforeAll task...");
            task.run(host);
        }
    }

    @Override
    public void runAfterAll(@NotNull LiveTestHost host) throws Exception {
        for (LiveTestTask task : afterAllTasks) {
            logger.info("[LIVETEST] Running afterAll task...");
            task.run(host);
        }
    }

    @Override
    public void runBeforeEach(@NotNull LiveTestHost host, @NotNull LiveTest test) throws Exception {
        for (LiveTestTask task : beforeEachTasks) {
            task.run(host);
        }
    }

    @Override
    public void runAfterEach(@NotNull LiveTestHost host, @NotNull LiveTest test) throws Exception {
        for (LiveTestTask task : afterEachTasks) {
            task.run(host);
        }
    }

    @Override
    public void delayIfNeeded() {
        if (afterAllDelayMs > 0) {
            logger.info("[LIVETEST] Waiting {} ms after afterAll tasks...", afterAllDelayMs);
            try {
                Thread.sleep(afterAllDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
