package de.t14d3.rapunzellib;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

final class RapunzelLeaseTest {
    @Test
    void closesOnlyWhenLastOwnerReleases(@TempDir Path dir) {
        Rapunzel.shutdownAll();

        Object ownerA = new Object();
        Object ownerB = new Object();
        TestContext ctx = new TestContext(dir);

        Rapunzel.Lease leaseA = Rapunzel.bootstrap(ownerA, ctx);
        assertSame(ctx, leaseA.context());
        assertEquals(1, Rapunzel.ownerCount());
        assertTrue(Rapunzel.isBootstrapped());
        assertFalse(ctx.isClosed());

        Rapunzel.Lease leaseB = Rapunzel.acquire(ownerB);
        assertSame(ctx, leaseB.context());
        assertEquals(2, Rapunzel.ownerCount());
        assertTrue(Rapunzel.isBootstrapped());

        Rapunzel.shutdown(ownerA);
        assertEquals(1, Rapunzel.ownerCount());
        assertTrue(Rapunzel.isBootstrapped());
        assertFalse(ctx.isClosed());

        Rapunzel.shutdown(ownerB);
        assertEquals(0, Rapunzel.ownerCount());
        assertFalse(Rapunzel.isBootstrapped());
        assertTrue(ctx.isClosed());
    }

    @Test
    void disallowsReplacingContext(@TempDir Path dir) {
        Rapunzel.shutdownAll();

        Object owner = new Object();
        TestContext ctx1 = new TestContext(dir.resolve("a"));
        TestContext ctx2 = new TestContext(dir.resolve("b"));

        Rapunzel.bootstrap(owner, ctx1);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> Rapunzel.bootstrap(new Object(), ctx2));
        assertTrue(e.getMessage().contains("already bootstrapped"));
    }

    @Test
    void bootstrapIsIdempotentPerOwner(@TempDir Path dir) {
        Rapunzel.shutdownAll();

        Object owner = new Object();
        TestContext ctx = new TestContext(dir);

        Rapunzel.Lease first = Rapunzel.bootstrap(owner, ctx);
        Rapunzel.Lease second = Rapunzel.bootstrap(owner, ctx);
        assertSame(first, second);
        assertEquals(1, Rapunzel.ownerCount());
    }

    @Test
    void bootstrapOrAcquireSkipsFactoryWhenAlreadyBootstrapped(@TempDir Path dir) {
        Rapunzel.shutdownAll();

        Object ownerA = new Object();
        Object ownerB = new Object();
        TestContext ctx = new TestContext(dir);

        Rapunzel.bootstrap(ownerA, ctx);

        Rapunzel.Lease leaseB = Rapunzel.bootstrapOrAcquire(ownerB, () -> {
            fail("contextFactory should not be invoked when already bootstrapped");
            return new TestContext(dir.resolve("unused"));
        });

        assertSame(ctx, leaseB.context());
        assertEquals(2, Rapunzel.ownerCount());
    }

    private static final class TestContext implements RapunzelContext {
        private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

        private final Path dataDir;
        private final AtomicBoolean closed = new AtomicBoolean();
        private final ServiceRegistry services = new MapServiceRegistry();
        private final Scheduler scheduler = new InlineScheduler();

        private TestContext(Path dataDir) {
            this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
        }

        boolean isClosed() {
            return closed.get();
        }

        @Override
        public @NotNull PlatformId platformId() {
            return PlatformId.PAPER;
        }

        @Override
        public @NotNull Logger logger() {
            return LOGGER;
        }

        @Override
        public @NotNull Path dataDirectory() {
            return dataDir;
        }

        @Override
        public @NotNull ResourceProvider resources() {
            return _path -> Optional.empty();
        }

        @Override
        public @NotNull Scheduler scheduler() {
            return scheduler;
        }

        @Override
        public @NotNull ServiceRegistry services() {
            return services;
        }

        @Override
        public void close() {
            closed.set(true);
        }
    }

    private static final class MapServiceRegistry implements ServiceRegistry {
        private final ConcurrentHashMap<Class<?>, Object> services = new ConcurrentHashMap<>();

        @Override
        public <T> void register(@NotNull Class<T> type, @NotNull T instance) {
            services.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(instance, "instance"));
        }

        @Override
        public <T> @NotNull Optional<T> find(@NotNull Class<T> type) {
            Object instance = services.get(Objects.requireNonNull(type, "type"));
            if (instance == null) return Optional.empty();
            return Optional.of(type.cast(instance));
        }

        @Override
        public @NotNull List<Class<?>> serviceTypes() {
            return services.keySet().stream().toList();
        }

        @Override
        public @NotNull List<Object> services() {
            return services.values().stream().toList();
        }
    }

    private static final class InlineScheduler implements Scheduler {
        @Override
        public @NotNull ScheduledTask run(@NotNull Runnable task) {
            task.run();
            return new NoopTask();
        }

        @Override
        public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
            task.run();
            return new NoopTask();
        }

        @Override
        public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
            task.run();
            return new NoopTask();
        }

        @Override
        public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            task.run();
            return new NoopTask();
        }

        @Override
        public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            task.run();
            return new NoopTask();
        }
    }

    private static final class NoopTask implements ScheduledTask {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
