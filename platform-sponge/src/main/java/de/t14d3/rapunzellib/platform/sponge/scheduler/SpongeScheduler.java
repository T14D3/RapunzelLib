package de.t14d3.rapunzellib.platform.sponge.scheduler;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Server;
import org.spongepowered.plugin.PluginContainer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class SpongeScheduler implements Scheduler, AutoCloseable {
    private final PluginContainer plugin;
    private final org.spongepowered.api.scheduler.Scheduler spongeScheduler;
    private final org.spongepowered.api.scheduler.Scheduler asyncSpongeScheduler;

    public SpongeScheduler(Server server, PluginContainer plugin) {
        Objects.requireNonNull(server, "server");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.spongeScheduler = server.scheduler();
        this.asyncSpongeScheduler = server.game().asyncScheduler();

    }

    @Override
    public @NotNull ScheduledTask run(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        return new FutureTaskHandle(spongeScheduler.executor(plugin).submit(task));
    }

    @Override
    public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        return new FutureTaskHandle(asyncSpongeScheduler.executor(plugin).submit(task));
    }

    @Override
    public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        long ms = Math.max(0L, (delay != null) ? delay.toMillis() : 0L);
        return new FutureTaskHandle(spongeScheduler.executor(plugin).schedule(task, ms, TimeUnit.MILLISECONDS));
    }

    @Override
    public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        long initialMs = Math.max(0L, initialDelay != null ? initialDelay.toMillis() : 0L);
        long periodMs = Math.max(1L, period != null ? period.toMillis() : 50L);

        return new FutureTaskHandle(spongeScheduler.executor(plugin).scheduleAtFixedRate(task, initialMs, periodMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        long initialMs = Math.max(0L, initialDelay != null ? initialDelay.toMillis() : 0L);
        long periodMs = Math.max(1L, period != null ? period.toMillis() : 50L);
        return new FutureTaskHandle(asyncSpongeScheduler.executor(plugin).scheduleAtFixedRate(task, initialMs, periodMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public void close() {
        // no-op
    }

    private record FutureTaskHandle(Future<?> future) implements ScheduledTask {
        @Override
        public void cancel() {
            if (future != null) future.cancel(false);
        }

        @Override
        public boolean isCancelled() {
            return future != null && future.isCancelled();
        }
    }
}
