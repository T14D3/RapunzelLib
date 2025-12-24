package de.t14d3.rapunzellib.platform.velocity.scheduler;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;

final class VelocityTaskHandle implements ScheduledTask {
    private final com.velocitypowered.api.scheduler.ScheduledTask task;

    VelocityTaskHandle(com.velocitypowered.api.scheduler.ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }

    @Override
    public boolean isCancelled() {
        return task.status() == TaskStatus.CANCELLED;
    }
}
