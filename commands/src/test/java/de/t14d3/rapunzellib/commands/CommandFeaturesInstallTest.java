package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class CommandFeaturesInstallTest {
    @BeforeEach
    void setUp() {
        Rapunzel.shutdownAll();
        TestCommandFeatureInstallers.reset();
    }

    @AfterEach
    void tearDown() {
        Rapunzel.shutdownAll();
    }

    @Test
    void accessorsLazyInstallCommandServices(@TempDir Path dir) {
        TestContext context = new TestContext(serverRuntime(PlatformId.PAPER, RuntimeCapability.COMMANDS), dir);
        Rapunzel.bootstrap(this, context);

        RCommandService commands = CommandFeatures.commands();

        assertSame(commands, CommandFeatures.install());
        assertSame(commands, context.services().get(RCommandService.class));
        assertSame(context.services().get(CommandSourceAdapters.class), CommandFeatures.adapters());
        assertEquals(1, TestCommandFeatureInstallers.paperInstallCalls());
    }

    @Test
    void accessorsRejectUnsupportedRuntime(@TempDir Path dir) {
        Rapunzel.bootstrap(this, new TestContext(proxyRuntime(), dir));

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            CommandFeatures::commands
        );

        assertEquals(0, TestCommandFeatureInstallers.paperInstallCalls());
        assertEquals(
            "RapunzelLib command features requires capability COMMANDS but runtime VELOCITY is PROXY / PROXY",
            ex.getMessage()
        );
    }

    private static PlatformRuntime proxyRuntime() {
        return new PlatformRuntime(
            PlatformId.VELOCITY,
            RuntimeRole.PROXY,
            EngineFamily.PROXY,
            EnumSet.noneOf(RuntimeCapability.class),
            new LifecycleOwner(new Object())
        );
    }

    private static PlatformRuntime serverRuntime(PlatformId platformId, RuntimeCapability... capabilities) {
        EnumSet<RuntimeCapability> capabilitySet = EnumSet.noneOf(RuntimeCapability.class);
        for (RuntimeCapability capability : capabilities) {
            capabilitySet.add(capability);
        }
        return new PlatformRuntime(
            platformId,
            RuntimeRole.SERVER,
            EngineFamily.MOJANG_SERVER,
            capabilitySet,
            new LifecycleOwner(new Object())
        );
    }

    private static final class TestContext implements RapunzelContext {
        private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

        private final PlatformRuntime runtime;
        private final Path dataDirectory;
        private final ServiceRegistry services = new MapServiceRegistry();
        private final Scheduler scheduler = new InlineScheduler();

        private TestContext(PlatformRuntime runtime, Path dataDirectory) {
            this.runtime = runtime;
            this.dataDirectory = dataDirectory;
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
            return dataDirectory;
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
    }

    private static final class MapServiceRegistry implements ServiceRegistry {
        private final ConcurrentHashMap<Class<?>, Object> services = new ConcurrentHashMap<>();

        @Override
        public <T> void register(@NotNull Class<T> type, @NotNull T instance) {
            services.put(type, instance);
        }

        @Override
        public <T> @NotNull Optional<T> find(@NotNull Class<T> type) {
            Object instance = services.get(type);
            if (instance == null) {
                return Optional.empty();
            }
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
