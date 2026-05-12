package de.t14d3.rapunzellib.common.scheduler;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;

/**
 * A scheduler wrapper that automatically binds each submitted task to a {@link RapunzelContext}.
 * <p>
 * Delegates scheduling to a platform-specific scheduler while wrapping every
 * {@link Runnable} to run within the given context via {@link Rapunzel#withContext}.
 * Also implements {@link AutoCloseable} to clean up the delegate if it is closeable.
 */
public final class ContextualScheduler implements Scheduler, AutoCloseable {
    /** The context to bind tasks to */
    private final RapunzelContext context;
    /** The underlying platform scheduler */
    private final Scheduler delegate;

    /**
     * Creates a contextual scheduler that binds tasks to the given context.
     *
     * @param context  the context to associate with all scheduled tasks
     * @param delegate the underlying platform scheduler
     */
    public ContextualScheduler(@NotNull RapunzelContext context, @NotNull Scheduler delegate) {
        this.context = Objects.requireNonNull(context, "context");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * Runs a task immediately on the scheduler's thread.
     *
     * @param task the task to run
     * @return a handle to the scheduled task
     */
    @Override
    public @NotNull ScheduledTask run(@NotNull Runnable task) {
        return delegate.run(wrap(task));
    }

    /**
     * Runs a task immediately on a scheduler-managed thread pool.
     *
     * @param task the task to run asynchronously
     * @return a handle to the scheduled task
     */
    @Override
    public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
        return delegate.runAsync(wrap(task));
    }

    /**
     * Runs a task after the specified delay on the scheduler's thread.
     *
     * @param delay the delay before execution
     * @param task  the task to run
     * @return a handle to the scheduled task
     */
    @Override
    public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
        return delegate.runLater(delay, wrap(task));
    }

    /**
     * Runs a task repeatedly with the given initial delay and period on the scheduler's thread.
     *
     * @param initialDelay the delay before the first execution
     * @param period       the interval between subsequent executions
     * @param task         the task to run
     * @return a handle to the scheduled task
     */
    @Override
    public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        return delegate.runRepeating(initialDelay, period, wrap(task));
    }

    /**
     * Runs a task repeatedly with the given initial delay and period on a scheduler-managed thread pool.
     *
     * @param initialDelay the delay before the first execution
     * @param period       the interval between subsequent executions
     * @param task         the task to run
     * @return a handle to the scheduled task
     */
    @Override
    public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        return delegate.runRepeatingAsync(initialDelay, period, wrap(task));
    }

    /**
     * Closes the delegate scheduler if it implements {@link AutoCloseable}.
     */
    @Override
    public void close() throws Exception {
        if (delegate instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    /**
     * Wraps a task to run within the configured context.
     *
     * @param task the original task
     * @return a context-bound runnable
     */
    private @NotNull Runnable wrap(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        return () -> Rapunzel.withContext(context, task);
    }
}
