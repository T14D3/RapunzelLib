package de.t14d3.rapunzellib.platform.velocity.scheduler;

import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class VelocityScheduler implements Scheduler {
    private final ProxyServer proxy;
    private final Object plugin;

    public VelocityScheduler(ProxyServer proxy, Object plugin) {
        this.proxy = Objects.requireNonNull(proxy, "proxy");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public ScheduledTask run(Runnable task) {
        return schedule(task, Duration.ZERO, null);
    }

    @Override
    public ScheduledTask runAsync(Runnable task) {
        return schedule(task, Duration.ZERO, null);
    }

    @Override
    public ScheduledTask runLater(Duration delay, Runnable task) {
        return schedule(task, delay, null);
    }

    @Override
    public ScheduledTask runRepeating(Duration initialDelay, Duration period, Runnable task) {
        return schedule(task, initialDelay, period);
    }

    @Override
    public ScheduledTask runRepeatingAsync(Duration initialDelay, Duration period, Runnable task) {
        return schedule(task, initialDelay, period);
    }

    private ScheduledTask schedule(Runnable task, Duration delay, Duration period) {
        var builder = proxy.getScheduler().buildTask(plugin, task);
        if (delay != null && !delay.isZero() && !delay.isNegative()) {
            builder.delay(delay.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (period != null && !period.isZero() && !period.isNegative()) {
            builder.repeat(period.toMillis(), TimeUnit.MILLISECONDS);
        }
        return new VelocityTaskHandle(builder.schedule());
    }
}

