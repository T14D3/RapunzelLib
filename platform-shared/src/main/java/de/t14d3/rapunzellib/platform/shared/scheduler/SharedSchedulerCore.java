package de.t14d3.rapunzellib.platform.shared.scheduler;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Abstract implementation of {@link Scheduler} that schedules tasks on the Minecraft server thread.
 * <p>
 * Uses a single-threaded {@link ScheduledExecutorService} for timing and delegates
 * task execution to {@link MinecraftServer#execute(Runnable)} for thread safety.
 * Implements {@link AutoCloseable} to shut down the executor service.
 * </p>
 */
public abstract class SharedSchedulerCore implements Scheduler, AutoCloseable {
    private final MinecraftServer server;
    private final ScheduledExecutorService timer;

    /**
     * Constructs a new scheduler core.
     *
     * @param server     the Minecraft server instance
     * @param threadName the name for the scheduler daemon thread
     */
    protected SharedSchedulerCore(MinecraftServer server, String threadName) {
        this.server = Objects.requireNonNull(server, "server");
        this.timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ScheduledTask run(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        if (server.isSameThread()) {
            task.run();
            return CompletedTask.INSTANCE;
        }
        return createTaskHandle(timer.schedule(() -> server.execute(task), 0L, TimeUnit.MILLISECONDS));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        return createTaskHandle(timer.submit(task));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        long ms = Math.max(0L, (delay != null) ? delay.toMillis() : 0L);
        return createTaskHandle(timer.schedule(() -> server.execute(task), ms, TimeUnit.MILLISECONDS));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        long initialMs = Math.max(0L, (initialDelay != null) ? initialDelay.toMillis() : 0L);
        long periodMs = Math.max(1L, (period != null) ? period.toMillis() : 50L);
        return createTaskHandle(timer.scheduleAtFixedRate(() -> server.execute(task), initialMs, periodMs, TimeUnit.MILLISECONDS));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        long initialMs = Math.max(0L, initialDelay != null ? initialDelay.toMillis() : 0L);
        long periodMs = Math.max(1L, period != null ? period.toMillis() : 50L);
        return createTaskHandle(timer.scheduleAtFixedRate(task, initialMs, periodMs, TimeUnit.MILLISECONDS));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        timer.shutdownNow();
    }

    /**
     * Creates a {@link ScheduledTask} handle from the given {@link Future}.
     *
     * @param future the future representing the scheduled task
     * @return a new ScheduledTask wrapper
     */
    protected @NotNull ScheduledTask createTaskHandle(Future<?> future) {
        return new SharedTaskHandle(future);
    }

    /**
     * Sentinel implementation of {@link ScheduledTask} for tasks that have already completed synchronously.
     */
    private static final class CompletedTask implements ScheduledTask {
        private static final CompletedTask INSTANCE = new CompletedTask();

        /**
         * No-op; the task has already run synchronously.
         */
        @Override
        public void cancel() {
            // already ran synchronously
        }

        /**
         * Returns {@code false} since the task completed successfully.
         *
         * @return {@code false} always
         */
        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
