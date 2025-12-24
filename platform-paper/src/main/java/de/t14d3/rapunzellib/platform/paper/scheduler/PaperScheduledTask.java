package de.t14d3.rapunzellib.platform.paper.scheduler;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

final class PaperScheduledTask implements ScheduledTask {
    private final BukkitTask task;

    PaperScheduledTask(BukkitTask task) {
        this.task = Objects.requireNonNull(task, "task");
    }

    @Override
    public void cancel() {
        task.cancel();
    }

    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }
}

