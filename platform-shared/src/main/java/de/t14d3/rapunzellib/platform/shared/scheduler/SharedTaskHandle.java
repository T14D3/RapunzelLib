package de.t14d3.rapunzellib.platform.shared.scheduler;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;

import java.util.Objects;
import java.util.concurrent.Future;

/**
 * Shared implementation of {@link ScheduledTask} wrapping a {@link Future}.
 * <p>
 * Provides cancellation and cancellation status by delegating to the underlying
 * {@link Future#cancel(boolean)} (with {@code mayInterruptIfRunning = false})
 * and {@link Future#isCancelled()} methods.
 * </p>
 */
public class SharedTaskHandle implements ScheduledTask {
    private final Future<?> future;

    /**
     * Constructs a new task handle.
     *
     * @param future the underlying Future to wrap
     */
    protected SharedTaskHandle(Future<?> future) {
        this.future = Objects.requireNonNull(future, "future");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() {
        future.cancel(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }
}
