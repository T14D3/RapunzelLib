package de.t14d3.rapunzellib.platform.paper.scheduler;

import de.t14d3.rapunzellib.scheduler.Scheduler;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Objects;

public final class PaperScheduler implements Scheduler {
    private final Plugin plugin;

    public PaperScheduler(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public ScheduledTask run(Runnable task) {
        return new PaperScheduledTask(Bukkit.getScheduler().runTask(plugin, task));
    }

    @Override
    public ScheduledTask runAsync(Runnable task) {
        return new PaperScheduledTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
    }

    @Override
    public ScheduledTask runLater(Duration delay, Runnable task) {
        return new PaperScheduledTask(Bukkit.getScheduler().runTaskLater(plugin, task, toTicks(delay)));
    }

    @Override
    public ScheduledTask runRepeating(Duration initialDelay, Duration period, Runnable task) {
        return new PaperScheduledTask(Bukkit.getScheduler().runTaskTimer(plugin, task, toTicks(initialDelay), toTicks(period)));
    }

    private static long toTicks(Duration duration) {
        if (duration == null) return 0L;
        long ms = duration.toMillis();
        if (ms <= 0L) return 0L;
        return (ms + 49L) / 50L;
    }
}

