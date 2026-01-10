package de.t14d3.rapunzellib.platform.neoforge.scheduler;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class NeoForgeScheduler implements Scheduler, AutoCloseable {
    private final MinecraftServer server;
    private final ScheduledExecutorService timer;

    public NeoForgeScheduler(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
        this.timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RapunzelLib-NeoForgeScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public @NotNull ScheduledTask run(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        if (server.isSameThread()) {
            task.run();
            return CompletedTask.INSTANCE;
        }
        return new NeoForgeTaskHandle(timer.schedule(() -> server.execute(task), 0L, TimeUnit.MILLISECONDS));
    }

    @Override
    public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        return new NeoForgeTaskHandle(timer.submit(task));
    }

    @Override
    public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        long ms = Math.max(0L, (delay != null) ? delay.toMillis() : 0L);
        return new NeoForgeTaskHandle(timer.schedule(() -> server.execute(task), ms, TimeUnit.MILLISECONDS));
    }

    @Override
    public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        long initialMs = Math.max(0L, (initialDelay != null) ? initialDelay.toMillis() : 0L);
        long periodMs = Math.max(1L, (period != null) ? period.toMillis() : 50L);
        return new NeoForgeTaskHandle(timer.scheduleAtFixedRate(() -> server.execute(task), initialMs, periodMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        long initialMs = Math.max(0L, initialDelay != null ? initialDelay.toMillis() : 0L);
        long periodMs = Math.max(1L, period != null ? period.toMillis() : 50L);
        return new NeoForgeTaskHandle(timer.scheduleAtFixedRate(task, initialMs, periodMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public void close() {
        timer.shutdownNow();
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
}
