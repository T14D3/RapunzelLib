package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GameEventBusTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameEventBusTest.class);

    @Test
    void dispatchPreStopsWhenDenied() {
        GameEventBus bus = new GameEventBus(new InlineScheduler(), LOGGER);
        AtomicInteger called = new AtomicInteger();

        bus.onPre(TestPreEvent.class, ev -> {
            called.incrementAndGet();
            ev.deny();
        });
        bus.onPre(TestPreEvent.class, _ev -> called.incrementAndGet());

        bus.dispatchPre(new TestPreEvent());
        assertEquals(1, called.get());
    }

    @Test
    void dispatchAsyncUsesScheduler() {
        AtomicInteger called = new AtomicInteger();
        GameEventBus bus = new GameEventBus(new InlineScheduler(), LOGGER);
        bus.onAsync(TestSnapshot.class, _ev -> called.incrementAndGet());

        bus.dispatchAsync(new TestSnapshot());
        assertEquals(1, called.get());
    }

    private static final class TestPreEvent extends BaseCancellablePreEvent {
    }

    private static final class TestSnapshot implements GameEventSnapshot {
    }

    private static final class InlineScheduler implements Scheduler {
        @Override
        public @NotNull ScheduledTask run(@NotNull Runnable task) {
            task.run();
            return NoopTask.INSTANCE;
        }

        @Override
        public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
            task.run();
            return NoopTask.INSTANCE;
        }

        @Override
        public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
            task.run();
            return NoopTask.INSTANCE;
        }

        @Override
        public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            task.run();
            return NoopTask.INSTANCE;
        }

        @Override
        public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            task.run();
            return NoopTask.INSTANCE;
        }
    }

    private enum NoopTask implements ScheduledTask {
        INSTANCE;

        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}

