package de.t14d3.rapunzellib.scheduler;

/**
 * Represents a scheduled task that can be cancelled and checked for cancellation status.
 */
public interface ScheduledTask {
    /**
     * Cancels this task. Implementations should treat cancellation as best-effort and idempotent.
     */
    void cancel();

    /** Returns whether this task has been cancelled. */
    boolean isCancelled();
}

