package de.t14d3.rapunzellib.common.scheduler;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;

public final class ContextualScheduler implements Scheduler, AutoCloseable {
    private final RapunzelContext context;
    private final Scheduler delegate;

    public ContextualScheduler(@NotNull RapunzelContext context, @NotNull Scheduler delegate) {
        this.context = Objects.requireNonNull(context, "context");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public @NotNull ScheduledTask run(@NotNull Runnable task) {
        return delegate.run(wrap(task));
    }

    @Override
    public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
        return delegate.runAsync(wrap(task));
    }

    @Override
    public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
        return delegate.runLater(delay, wrap(task));
    }

    @Override
    public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        return delegate.runRepeating(initialDelay, period, wrap(task));
    }

    @Override
    public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
        return delegate.runRepeatingAsync(initialDelay, period, wrap(task));
    }

    @Override
    public void close() throws Exception {
        if (delegate instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    private @NotNull Runnable wrap(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        return () -> Rapunzel.withContext(context, task);
    }
}
