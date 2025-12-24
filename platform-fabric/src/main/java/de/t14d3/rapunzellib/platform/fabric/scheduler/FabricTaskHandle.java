package de.t14d3.rapunzellib.platform.fabric.scheduler;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;

import java.util.Objects;
import java.util.concurrent.Future;

final class FabricTaskHandle implements ScheduledTask {
    private final Future<?> future;

    FabricTaskHandle(Future<?> future) {
        this.future = Objects.requireNonNull(future, "future");
    }

    @Override
    public void cancel() {
        future.cancel(false);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }
}

