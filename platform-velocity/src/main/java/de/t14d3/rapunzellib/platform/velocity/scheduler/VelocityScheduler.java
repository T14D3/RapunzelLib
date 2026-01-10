package de.t14d3.rapunzellib.platform.velocity.scheduler;

import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull ScheduledTask run(@NotNull Runnable task) {
        return schedule(task, Duration.ZERO, null);
    }

    @Override
    public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
        return schedule(task, Duration.ZERO, null);
    }

    @Override
    public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
        return schedule(task, delay, null);
    }

    @Override
    public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        return schedule(task, initialDelay, period);
    }

    @Override
    public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
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

