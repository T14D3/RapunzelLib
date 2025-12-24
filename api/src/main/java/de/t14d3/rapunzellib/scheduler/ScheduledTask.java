package de.t14d3.rapunzellib.scheduler;

public interface ScheduledTask {
    void cancel();

    boolean isCancelled();
}

