package de.t14d3.rapunzellib.platform.paper.scheduler;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;

import java.util.Objects;
import java.util.concurrent.Future;

final class PaperFutureTaskHandle implements ScheduledTask {
    private final Future<?> future;

    PaperFutureTaskHandle(Future<?> future) {
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

