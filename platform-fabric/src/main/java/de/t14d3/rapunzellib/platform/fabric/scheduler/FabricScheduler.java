package de.t14d3.rapunzellib.platform.fabric.scheduler;

import de.t14d3.rapunzellib.scheduler.Scheduler;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class FabricScheduler implements Scheduler, AutoCloseable {
    private final MinecraftServer server;
    private final ScheduledExecutorService timer;

    public FabricScheduler(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
        this.timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RapunzelLib-FabricScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public ScheduledTask run(Runnable task) {
        return new FabricTaskHandle(timer.schedule(() -> server.execute(task), 0L, TimeUnit.MILLISECONDS));
    }

    @Override
    public ScheduledTask runAsync(Runnable task) {
        return new FabricTaskHandle(timer.submit(task));
    }

    @Override
    public ScheduledTask runLater(Duration delay, Runnable task) {
        long ms = Math.max(0L, (delay != null) ? delay.toMillis() : 0L);
        return new FabricTaskHandle(timer.schedule(() -> server.execute(task), ms, TimeUnit.MILLISECONDS));
    }

    @Override
    public ScheduledTask runRepeating(Duration initialDelay, Duration period, Runnable task) {
        long initialMs = Math.max(0L, (initialDelay != null) ? initialDelay.toMillis() : 0L);
        long periodMs = Math.max(1L, (period != null) ? period.toMillis() : 50L);
        return new FabricTaskHandle(timer.scheduleAtFixedRate(() -> server.execute(task), initialMs, periodMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public void close() {
        timer.shutdown();
    }
}

