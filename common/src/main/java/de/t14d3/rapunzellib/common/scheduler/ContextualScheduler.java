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
 * Delegates scheduling to a platform-specific scheduler. In the single-context model,
 * {@link Rapunzel#context()} always returns the global context, so no explicit scoping
 * wrapping is needed. This class exists to implement {@link AutoCloseable} for cleanup
 * and to document the context association.</p>
 */
public final class ContextualScheduler implements Scheduler, AutoCloseable {
    private final RapunzelContext context;
    private final Scheduler delegate;

    public ContextualScheduler(@NotNull RapunzelContext context, @NotNull Scheduler delegate) {
        this.context = Objects.requireNonNull(context, "context");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * Schedules a task to run on the platform's primary thread, bound to the context.
     *
     * @param task the task to execute within the context scope
     * @return a {@link ScheduledTask} handle
     */
    @Override
    public @NotNull ScheduledTask run(@NotNull Runnable task) {
        return delegate.run(task);
    }

    @Override
    public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
        return delegate.runAsync(task);
    }

    /**
     * Schedules a task to run after the specified delay, bound to the context.
     *
     * @param delay the delay before execution
     * @param task  the task to execute within the context scope
     * @return a {@link ScheduledTask} handle
     */
    @Override
    public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
        return delegate.runLater(delay, task);
    }

    @Override
    public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        return delegate.runRepeating(initialDelay, period, task);
    }

    @Override
    public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        return delegate.runRepeatingAsync(initialDelay, period, task);
    }

    @Override
    public void close() throws Exception {
        if (delegate instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
