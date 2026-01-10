package de.t14d3.rapunzellib.scheduler;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public interface Scheduler {
    /**
     * Schedules {@code task} to run on the platform's primary thread (when applicable).
     */
    @NotNull ScheduledTask run(@NotNull Runnable task);

    /**
     * Schedules {@code task} to run asynchronously (off the primary thread).
     */
    @NotNull ScheduledTask runAsync(@NotNull Runnable task);

    /**
     * Schedules {@code task} to run after {@code delay}.
     */
    @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task);

    /**
     * Schedules {@code task} to run repeatedly on the primary thread (when applicable).
     */
    @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task);

    /**
     * Schedules {@code task} to run repeatedly asynchronously.
     */
    @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task);
}

