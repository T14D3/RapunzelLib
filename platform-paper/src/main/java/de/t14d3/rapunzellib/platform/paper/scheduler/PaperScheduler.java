package de.t14d3.rapunzellib.platform.paper.scheduler;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class PaperScheduler implements Scheduler, AutoCloseable {
    private final Plugin plugin;
    private final MinecraftServer server;
    private final Thread mainThread;
    private final ScheduledThreadPoolExecutor asyncExecutor;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public PaperScheduler(Plugin plugin) {
        this(plugin, defaultAsyncThreads());
    }

    public PaperScheduler(Plugin plugin, int asyncThreads) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.server = ((CraftServer) plugin.getServer()).getServer();
        this.mainThread = this.server.serverThread;
        this.asyncExecutor = createAsyncExecutor(plugin, asyncThreads);
    }

    @Override
    public ScheduledTask run(Runnable task) {
        Objects.requireNonNull(task, "task");
        ensureOpen();
        if (Thread.currentThread() == mainThread) {
            runSafely(task, "run");
            return CompletedTask.INSTANCE;
        }
        MainThreadTaskHandle handle = new MainThreadTaskHandle(task, "run");
        server.execute(handle);
        return handle;
    }

    @Override
    public ScheduledTask runAsync(Runnable task) {
        Objects.requireNonNull(task, "task");
        ensureOpen();
        return new PaperFutureTaskHandle(asyncExecutor.submit(wrapAsync(task, "runAsync")));
    }

    @Override
    public ScheduledTask runLater(Duration delay, Runnable task) {
        Objects.requireNonNull(task, "task");
        ensureOpen();
        long delayMs = safeDelayMs(delay);
        MainThreadTaskHandle handle = new MainThreadTaskHandle(task, "runLater");
        ScheduledFuture<?> future = asyncExecutor.schedule(() -> server.execute(handle), delayMs, TimeUnit.MILLISECONDS);
        return new ScheduledFutureHandle(handle, future);
    }

    @Override
    public ScheduledTask runRepeating(Duration initialDelay, Duration period, Runnable task) {
        Objects.requireNonNull(task, "task");
        ensureOpen();
        long initialMs = safeDelayMs(initialDelay);
        long periodMs = safePeriodMs(period);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        ScheduledFuture<?> future = asyncExecutor.scheduleAtFixedRate(() -> {
            if (cancelled.get()) return;
            server.execute(new MainThreadTaskHandle(task, "runRepeating"));
        }, initialMs, periodMs, TimeUnit.MILLISECONDS);
        return new RepeatingSyncTaskHandle(cancelled, future);
    }

    @Override
    public ScheduledTask runRepeatingAsync(Duration initialDelay, Duration period, Runnable task) {
        Objects.requireNonNull(task, "task");
        ensureOpen();
        long initialMs = safeDelayMs(initialDelay);
        long periodMs = safePeriodMs(period);
        return new PaperFutureTaskHandle(
                asyncExecutor.scheduleAtFixedRate(wrapAsync(task, "runRepeatingAsync"), initialMs, periodMs, TimeUnit.MILLISECONDS)
        );
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        asyncExecutor.shutdownNow();
    }

    private void runSafely(Runnable task, String label) {
        try {
            task.run();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Unhandled exception in " + label + " task", t);
        }
    }

    private Runnable wrapAsync(Runnable task, String label) {
        return () -> runSafely(task, label);
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Scheduler is closed");
        }
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

    private static final class CompletedTask implements ScheduledTask {
        private static final CompletedTask INSTANCE = new CompletedTask();

        @Override
        public void cancel() {
            // already ran synchronously
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    private final class MainThreadTaskHandle implements ScheduledTask, Runnable {
        private final Runnable task;
        private final String label;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        MainThreadTaskHandle(Runnable task, String label) {
            this.task = Objects.requireNonNull(task, "task");
            this.label = label;
        }

        @Override
        public void run() {
            if (cancelled.get() || closed.get()) return;
            runSafely(task, label);
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    private final class ScheduledFutureHandle implements ScheduledTask {
        private final MainThreadTaskHandle handle;
        private final ScheduledFuture<?> future;

        ScheduledFutureHandle(MainThreadTaskHandle handle, ScheduledFuture<?> future) {
            this.handle = handle;
            this.future = future;
        }

        @Override
        public void cancel() {
            handle.cancel();
            future.cancel(false);
        }

        @Override
        public boolean isCancelled() {
            return handle.isCancelled() || future.isCancelled();
        }
    }

    private record RepeatingSyncTaskHandle(AtomicBoolean cancelled, ScheduledFuture<?> future) implements ScheduledTask {

        @Override
            public void cancel() {
                cancelled.set(true);
                future.cancel(false);
            }

            @Override
            public boolean isCancelled() {
                return cancelled.get() || future.isCancelled();
            }
        }
}
