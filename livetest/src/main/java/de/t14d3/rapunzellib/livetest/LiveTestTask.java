package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

/**
 * A task that runs as part of the live test lifecycle (beforeAll, afterAll,
 * beforeEach, afterEach).
 *
 * @see LiveTestTaskManager
 */
@FunctionalInterface
public interface LiveTestTask {

    /**
     * Executes this task.
     *
     * @param host the live test host, for dispatching commands or logging
     * @throws Exception if the task fails
     */
    void run(@NotNull LiveTestHost host) throws Exception;
}
