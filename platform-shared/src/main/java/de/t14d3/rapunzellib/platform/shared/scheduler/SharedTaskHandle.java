package de.t14d3.rapunzellib.platform.shared.scheduler;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;

import java.util.Objects;
import java.util.concurrent.Future;

/** Shared implementation of {@link ScheduledTask} wrapping a {@link Future}. */
public class SharedTaskHandle implements ScheduledTask {
    private final Future<?> future;

    protected SharedTaskHandle(Future<?> future) {
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
