package de.t14d3.rapunzellib.platform.paper.scheduler;

import de.t14d3.rapunzellib.scheduler.Scheduler;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class PaperScheduler implements Scheduler, AutoCloseable {
    private final Plugin plugin;
    private final ScheduledThreadPoolExecutor asyncExecutor;

    public PaperScheduler(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.asyncExecutor = createAsyncExecutor(plugin, defaultAsyncThreads());
    }

    public PaperScheduler(Plugin plugin, int asyncThreads) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.asyncExecutor = createAsyncExecutor(plugin, asyncThreads);
    }

    @Override
    public ScheduledTask run(Runnable task) {
        Objects.requireNonNull(task, "task");
        return new PaperScheduledTask(Bukkit.getScheduler().runTask(plugin, task));
    }

    @Override
    public ScheduledTask runAsync(Runnable task) {
        Objects.requireNonNull(task, "task");
        return new PaperFutureTaskHandle(asyncExecutor.submit(wrapAsync(task, "runAsync")));
    }

    @Override
    public ScheduledTask runLater(Duration delay, Runnable task) {
        Objects.requireNonNull(task, "task");
        return new PaperScheduledTask(Bukkit.getScheduler().runTaskLater(plugin, task, toTicks(delay)));
    }

    @Override
    public ScheduledTask runRepeating(Duration initialDelay, Duration period, Runnable task) {
        Objects.requireNonNull(task, "task");
        return new PaperScheduledTask(Bukkit.getScheduler().runTaskTimer(plugin, task, toTicks(initialDelay), toTicks(period)));
    }

    @Override
    public ScheduledTask runRepeatingAsync(Duration initialDelay, Duration period, Runnable task) {
        Objects.requireNonNull(task, "task");
        long initialMs = safeDelayMs(initialDelay);
        long periodMs = safePeriodMs(period);
        return new PaperFutureTaskHandle(
            asyncExecutor.scheduleAtFixedRate(wrapAsync(task, "runRepeatingAsync"), initialMs, periodMs, TimeUnit.MILLISECONDS)
        );
    }

    @Override
    public void close() {
        asyncExecutor.shutdownNow();
    }

    private Runnable wrapAsync(Runnable task, String label) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Unhandled exception in " + label + " task", t);
            }
        };
    }

    private static long safeDelayMs(Duration delay) {
        if (delay == null) return 0L;
        long ms = delay.toMillis();
        return Math.max(0L, ms);
    }

    private static long safePeriodMs(Duration period) {
        if (period == null) return 50L;
        long ms = period.toMillis();
        return Math.max(1L, ms);
    }

    private static int defaultAsyncThreads() {
        int cpus = Runtime.getRuntime().availableProcessors();
        return Math.max(2, Math.min(4, cpus));
    }

    private static ScheduledThreadPoolExecutor createAsyncExecutor(Plugin plugin, int asyncThreads) {
        int threads = Math.max(1, asyncThreads);
        String baseName = "RapunzelLib-PaperScheduler-" + plugin.getName();
        AtomicInteger idx = new AtomicInteger();

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(threads, r -> {
            Thread t = new Thread(r, baseName + "-" + idx.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    private static long toTicks(Duration duration) {
        if (duration == null) return 0L;
        long ms = duration.toMillis();
        if (ms <= 0L) return 0L;
        return (ms + 49L) / 50L;
    }
}

