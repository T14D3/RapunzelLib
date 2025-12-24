package de.t14d3.rapunzellib;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
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
        public PlatformId platformId() {
            return PlatformId.PAPER;
        }

        @Override
        public Logger logger() {
            return LOGGER;
        }

        @Override
        public Path dataDirectory() {
            return dataDir;
        }

        @Override
        public ResourceProvider resources() {
            return _path -> Optional.empty();
        }

        @Override
        public Scheduler scheduler() {
            return scheduler;
        }

        @Override
        public ServiceRegistry services() {
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
        public <T> void register(Class<T> type, T instance) {
            services.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(instance, "instance"));
        }

        @Override
        public <T> Optional<T> find(Class<T> type) {
            Object instance = services.get(Objects.requireNonNull(type, "type"));
            if (instance == null) return Optional.empty();
            return Optional.of(type.cast(instance));
        }
    }

    private static final class InlineScheduler implements Scheduler {
        @Override
        public ScheduledTask run(Runnable task) {
            task.run();
            return new NoopTask();
        }

        @Override
        public ScheduledTask runAsync(Runnable task) {
            task.run();
            return new NoopTask();
        }

        @Override
        public ScheduledTask runLater(Duration delay, Runnable task) {
            task.run();
            return new NoopTask();
        }

        @Override
        public ScheduledTask runRepeating(Duration initialDelay, Duration period, Runnable task) {
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
