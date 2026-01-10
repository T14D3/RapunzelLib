package de.t14d3.rapunzellib.scheduler;

public interface ScheduledTask {
    /**
     * Cancels this task.
     *
     * <p>Implementations should treat cancellation as best-effort and idempotent.</p>
     */
    void cancel();

    boolean isCancelled();
}

