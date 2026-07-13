package de.t14d3.rapunzellib.scheduler;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public interface Scheduler {
    /**
     * Schedules a task to run on the platform's primary thread (when applicable).
     *
     * @param task the task to execute
     * @return a {@link ScheduledTask} handle for the scheduled task
     */
    @NotNull ScheduledTask run(@NotNull Runnable task);

    /** Schedules {@code task} to run asynchronously (off the primary thread). */
    @NotNull ScheduledTask runAsync(@NotNull Runnable task);

    /**
     * Schedules a task to run once after the specified delay on the primary thread.
     *
     * @param delay the delay before the task runs
     * @param task  the task to execute
     * @return a {@link ScheduledTask} handle for the scheduled task
     */
    @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task);

    /**
     * Schedules a task to run repeatedly on the primary thread with an initial delay and period.
     *
     * @param initialDelay the delay before the first execution
     * @param period       the interval between subsequent executions
     * @param task         the task to execute
     * @return a {@link ScheduledTask} handle for the scheduled task
     */
    @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task);

    /** Schedules {@code task} to run repeatedly asynchronously. */
    @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task);
}

