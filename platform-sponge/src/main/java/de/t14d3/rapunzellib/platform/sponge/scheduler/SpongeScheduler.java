package de.t14d3.rapunzellib.platform.sponge.scheduler;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SpongeScheduler implements Scheduler, AutoCloseable {
    private final ScheduledExecutorService executor;
    private final Server server;

    public SpongeScheduler(Server server) {
        Objects.requireNonNull(server, "server");
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RapunzelLib-SpongeScheduler");
            t.setDaemon(true);
            return t;
        });

        this.server = server;
    }

    @Override
    public ScheduledTask run(Runnable task) {
        Objects.requireNonNull(task, "task");
        task.run();
        return new FutureTaskHandle(null);
    }

    @Override
    public ScheduledTask runAsync(Runnable task) {
        Objects.requireNonNull(task, "task");
        return new FutureTaskHandle(executor.submit(task));
    }

    @Override
    public ScheduledTask runLater(Duration delay, Runnable task) {
        Objects.requireNonNull(task, "task");
        long ms = Math.max(0L, (delay != null) ? delay.toMillis() : 0L);
        return new FutureTaskHandle(executor.schedule(task, ms, TimeUnit.MILLISECONDS));
    }

    @Override
    public ScheduledTask runRepeating(Duration initialDelay, Duration period, Runnable task) {
        return runRepeatingAsync(initialDelay, period, task);
    }

    @Override
    public ScheduledTask runRepeatingAsync(Duration initialDelay, Duration period, Runnable task) {
        Objects.requireNonNull(task, "task");
        long initialMs = Math.max(0L, initialDelay != null ? initialDelay.toMillis() : 0L);
        long periodMs = Math.max(1L, period != null ? period.toMillis() : 50L);
        return new FutureTaskHandle(executor.scheduleAtFixedRate(task, initialMs, periodMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public void close() {
        executor.shutdownNow();
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
