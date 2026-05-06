package de.t14d3.rapunzellib;

import de.t14d3.rapunzellib.bootstrap.BootstrapHandle;
import de.t14d3.rapunzellib.bootstrap.BootstrapOwnerRole;
import de.t14d3.rapunzellib.bootstrap.PlatformBootstrapHost;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

final class RapunzelLeaseTest {
    @Test
    void laterBootstrapCallsCreateIndependentConsumerContexts(@TempDir Path dir) {
        resetBootstrap();

        Object ownerA = new Object();
        Object ownerB = new Object();
        TestContext ctx = new TestContext(dir.resolve("owner"));
        TestContext ctxB = new TestContext(dir.resolve("consumer-b"));

        BootstrapHandle ownerHandle = Rapunzel.bootstrap(ownerA, ctx);
        BootstrapHandle consumerBHandle = Rapunzel.bootstrap(ownerB, ctxB);

        assertSame(ctx, ownerHandle.context());
        assertEquals(BootstrapOwnerRole.OWNER, ownerHandle.role());
        assertEquals(2, Rapunzel.ownerCount());
        assertEquals(0, Rapunzel.borrowerCount());
        assertTrue(Rapunzel.isBootstrapped());
        assertFalse(ctx.isClosed());

        assertSame(ctxB, consumerBHandle.context());
        assertEquals(BootstrapOwnerRole.OWNER, consumerBHandle.role());
        assertFalse(ctxB.isClosed());

        Rapunzel.shutdown(ownerB);
        assertEquals(1, Rapunzel.ownerCount());
        assertEquals(0, Rapunzel.borrowerCount());
        assertTrue(Rapunzel.isBootstrapped());
        assertFalse(ctx.isClosed());
        assertTrue(ctxB.isClosed());

        Rapunzel.shutdown(ownerA);
        assertFalse(Rapunzel.isBootstrapped());
        assertTrue(ctx.isClosed());
    }

    @Test
    void registeredHostDoesNotStealConsumerOwnership(@TempDir Path dir) {
        resetBootstrap();

        Object borrower = new Object();
        TestContext ctx = new TestContext(dir);
        TestPlatformHost host = new TestPlatformHost(true);
        Rapunzel.registerPlatformBootstrapHost(host);

        BootstrapHandle handle = Rapunzel.bootstrap(borrower, () -> ctx);

        assertEquals(BootstrapOwnerRole.OWNER, handle.role());
        assertSame(ctx, handle.context());
        assertEquals(1, Rapunzel.ownerCount());
        assertEquals(0, Rapunzel.borrowerCount());
        assertSame(borrower, Rapunzel.bootstrapState().ownerToken().orElseThrow());

        Rapunzel.shutdown(borrower);
        assertFalse(Rapunzel.isBootstrapped());
        assertTrue(ctx.isClosed());
    }

    @Test
    void canonicalHostCallerKeepsOwnerHandle(@TempDir Path dir) {
        resetBootstrap();

        TestContext ctx = new TestContext(dir);
        TestPlatformHost host = new TestPlatformHost(true);
        Rapunzel.registerPlatformBootstrapHost(host);

        BootstrapHandle handle = Rapunzel.bootstrap(host.ownerToken(), () -> ctx);

        assertEquals(BootstrapOwnerRole.OWNER, handle.role());
        assertSame(host.ownerToken(), handle.participant());
        assertEquals(1, Rapunzel.ownerCount());
        assertEquals(0, Rapunzel.borrowerCount());
    }

    @Test
    void fallsBackToEmbeddedOwnerWhenHostCannotClaim(@TempDir Path dir) {
        resetBootstrap();

        Object owner = new Object();
        TestContext ctx = new TestContext(dir);
        Rapunzel.registerPlatformBootstrapHost(new TestPlatformHost(false));

        BootstrapHandle handle = Rapunzel.bootstrap(owner, () -> ctx);

        assertEquals(BootstrapOwnerRole.OWNER, handle.role());
        assertSame(owner, Rapunzel.bootstrapState().ownerToken().orElseThrow());
        assertSame(ctx, handle.context());
    }

    @Test
    void bootstrapIsIdempotentPerCaller(@TempDir Path dir) {
        resetBootstrap();

        Object owner = new Object();
        TestContext ctx = new TestContext(dir);

        BootstrapHandle first = Rapunzel.bootstrap(owner, ctx);
        BootstrapHandle second = Rapunzel.bootstrap(owner, ctx);
        assertSame(first, second);
        assertEquals(1, Rapunzel.ownerCount());
        assertEquals(0, Rapunzel.borrowerCount());
    }

    @Test
    void bootstrapOrAcquireCreatesIndependentContextForNewOwner(@TempDir Path dir) {
        resetBootstrap();

        Object ownerA = new Object();
        Object ownerB = new Object();
        TestContext ctx = new TestContext(dir);
        TestContext ctxB = new TestContext(dir.resolve("consumer-b"));

        Rapunzel.bootstrap(ownerA, ctx);

        BootstrapHandle leaseB = Rapunzel.bootstrapOrAcquire(ownerB, () -> ctxB);

        assertSame(ctxB, leaseB.context());
        assertEquals(BootstrapOwnerRole.OWNER, leaseB.role());
        assertEquals(2, Rapunzel.ownerCount());
        assertEquals(0, Rapunzel.borrowerCount());
    }

    @Test
    void staticConvenienceHelpersDelegateToContext(@TempDir Path dir) {
        resetBootstrap();

        Object owner = new Object();
        TestContext ctx = new TestContext(dir);
        Rapunzel.bootstrap(owner, ctx);

        assertSame(ctx.services(), Rapunzel.services());
        assertSame(ctx.scheduler(), Rapunzel.scheduler());
        assertSame(ctx.configs(), Rapunzel.configs());
        assertSame(ctx.messages(), Rapunzel.messages());
        assertSame(ctx.logger(), Rapunzel.logger());
        assertSame(ctx.dataDirectory(), Rapunzel.dataDirectory());
        assertSame(ctx.runtime(), Rapunzel.runtime());
        assertSame(ctx.platformId(), Rapunzel.platformId());

        ExampleService service = Rapunzel.service(ExampleService.class);
        assertSame(ctx.exampleService(), service);
        assertSame(service, Rapunzel.findService(ExampleService.class).orElseThrow());
        assertTrue(Rapunzel.findService(MissingService.class).isEmpty());
    }

    @Test
    void staticContextIsAmbiguousWithoutScopeWhenMultipleConsumersExist(@TempDir Path dir) {
        resetBootstrap();

        TestContext ctxA = new TestContext(dir.resolve("a"));
        TestContext ctxB = new TestContext(dir.resolve("b"));
        Rapunzel.bootstrap("a", ctxA);
        Rapunzel.bootstrap("b", ctxB);

        assertTrue(Rapunzel.findContext().isEmpty());
        assertThrows(IllegalStateException.class, Rapunzel::context);
    }

    @Test
    void scopedContextRestoresPreviousContext(@TempDir Path dir) {
        resetBootstrap();

        TestContext ctxA = new TestContext(dir.resolve("a"));
        TestContext ctxB = new TestContext(dir.resolve("b"));
        Rapunzel.bootstrap("a", ctxA);
        Rapunzel.bootstrap("b", ctxB);

        Rapunzel.withContext(ctxA, () -> {
            assertSame(ctxA, Rapunzel.context());
            Rapunzel.withContext(ctxB, () -> assertSame(ctxB, Rapunzel.context()));
            assertSame(ctxA, Rapunzel.context());
        });
        assertTrue(Rapunzel.findContext().isEmpty());
    }

    private static void resetBootstrap() {
        Rapunzel.shutdownAll();
        Rapunzel.clearRegisteredPlatformBootstrapHost();
    }

    private static final class TestPlatformHost implements PlatformBootstrapHost {
        private final Object ownerToken = new Object();
        private final boolean available;

        private TestPlatformHost(boolean available) {
            this.available = available;
        }

        @Override
        public @NotNull Object ownerToken() {
            return ownerToken;
        }

        @Override
        public @NotNull String displayName() {
            return "test-host";
        }

        @Override
        public @NotNull Optional<? extends RapunzelContext> tryCreateContext(
            @NotNull Object bootstrapCaller,
            @NotNull Supplier<? extends RapunzelContext> contextFactory
        ) {
            Objects.requireNonNull(bootstrapCaller, "bootstrapCaller");
            Objects.requireNonNull(contextFactory, "contextFactory");
            if (!available) {
                return Optional.empty();
            }
            return Optional.of(contextFactory.get());
        }
    }

    private static final class TestContext implements RapunzelContext {
        private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);
        private static final ConfigService CONFIGS = new TestConfigService();
        private static final MessageFormatService MESSAGES = new TestMessageFormatService();

        private final Path dataDir;
        private final PlatformRuntime runtime = new PlatformRuntime(
            PlatformId.PAPER,
            RuntimeRole.SERVER,
            EngineFamily.MOJANG_SERVER,
            EnumSet.of(RuntimeCapability.WORLDS, RuntimeCapability.BLOCKS),
            new LifecycleOwner(this)
        );
        private final AtomicBoolean closed = new AtomicBoolean();
        private final ServiceRegistry services = new MapServiceRegistry();
        private final Scheduler scheduler = new InlineScheduler();
        private final ExampleService exampleService = new ExampleServiceImpl();

        private TestContext(Path dataDir) {
            this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
            services.register(ConfigService.class, CONFIGS);
            services.register(MessageFormatService.class, MESSAGES);
            services.register(ExampleService.class, exampleService);
        }

        boolean isClosed() {
            return closed.get();
        }

        ExampleService exampleService() {
            return exampleService;
        }

        @Override
        public @NotNull PlatformRuntime runtime() {
            return runtime;
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

    private interface ExampleService {
        String value();
    }

    private interface MissingService {
    }

    private static final class ExampleServiceImpl implements ExampleService {
        @Override
        public String value() {
            return "example";
        }
    }

    private static final class TestConfigService implements ConfigService {
        @Override
        public @NotNull YamlConfig load(@NotNull Path file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull YamlConfig load(@NotNull Path file, @NotNull String defaultResourcePath) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestMessageFormatService implements MessageFormatService {
        @Override
        public void reload() {
        }

        @Override
        public boolean contains(@NotNull String key) {
            return false;
        }

        @Override
        public @NotNull Set<String> keys() {
            return Set.of();
        }

        @Override
        public @NotNull String raw(@NotNull String key) {
            return key;
        }

        @Override
        public @NotNull Component component(@NotNull String key) {
            return Component.text(key);
        }

        @Override
        public @NotNull Component component(@NotNull String key, @NotNull Placeholders placeholders) {
            return Component.text(key);
        }
    }
}
