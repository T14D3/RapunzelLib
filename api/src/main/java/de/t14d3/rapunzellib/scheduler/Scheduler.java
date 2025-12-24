package de.t14d3.rapunzellib.scheduler;

import java.time.Duration;

public interface Scheduler {
    ScheduledTask run(Runnable task);

    ScheduledTask runAsync(Runnable task);

    ScheduledTask runLater(Duration delay, Runnable task);

    ScheduledTask runRepeating(Duration initialDelay, Duration period, Runnable task);
}

