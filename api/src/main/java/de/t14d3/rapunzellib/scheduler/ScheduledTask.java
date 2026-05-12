package de.t14d3.rapunzellib.scheduler;

/**
 * Represents a scheduled task that can be cancelled and checked for cancellation status.
 */
public interface ScheduledTask {
    /**
     * Cancels this task.
     *
     * <p>Implementations should treat cancellation as best-effort and idempotent.</p>
     */
    void cancel();

    /**
     * Returns whether this task has been cancelled.
     *
     * @return true if cancelled, false otherwise
     */
    boolean isCancelled();
}

