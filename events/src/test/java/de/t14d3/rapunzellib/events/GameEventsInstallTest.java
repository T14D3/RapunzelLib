package de.t14d3.rapunzellib.events;

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
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GameEventsInstallTest {
    @AfterEach
    void tearDown() {
        TestGameEventBridgeInstaller.reset();
        Rapunzel.shutdownAll();
    }

    @Test
    void rejectsUnsupportedRuntimeBeforeResolvingInstaller(@TempDir Path dir) {
        Rapunzel.bootstrap(this, new TestContext(proxyRuntime(), dir));

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            GameEvents::install
        );

        assertEquals("RapunzelLib game events requires capability EVENTS but runtime VELOCITY is PROXY / PROXY", ex.getMessage());
    }

    @Test
    void discoversSupportWithoutInstallSideEffects(@TempDir Path dir) {
        TestContext context = new TestContext(serverRuntime(PlatformId.PAPER, RuntimeCapability.EVENTS), dir);
        Rapunzel.bootstrap(this, context);

        GameEventSupportManifest currentManifest = GameEvents.support();
        GameEventSupportManifest runtimeManifest = GameEvents.support(serverRuntime(PlatformId.PAPER, RuntimeCapability.EVENTS));
        GameEventSupportManifest platformManifest = GameEvents.support(PlatformId.PAPER);

        assertEquals(TestGameEventBridgeInstaller.MANIFEST.overlayUnsupported(TestInventoryGameEventSupportContributor.MANIFEST), currentManifest);
        assertEquals(TestGameEventBridgeInstaller.MANIFEST.overlayUnsupported(TestInventoryGameEventSupportContributor.MANIFEST), runtimeManifest);
        assertEquals(TestGameEventBridgeInstaller.MANIFEST.overlayUnsupported(TestInventoryGameEventSupportContributor.MANIFEST), platformManifest);
        assertEquals(0, TestGameEventBridgeInstaller.installCalls());
        assertFalse(context.services().find(GameEventBus.class).isPresent());
        assertFalse(context.services().find(GameEventBridge.class).isPresent());
        assertFalse(context.services().find(GameEventSupportManifest.class).isPresent());
        assertTrue(platformManifest.supports(de.t14d3.rapunzellib.events.block.BlockBreakPre.class));
        assertEquals(GameEventSupportParity.NATIVE, platformManifest.support(de.t14d3.rapunzellib.events.block.BlockBreakPre.class).parity());
        assertEquals(GameEventSupportParity.UNSUPPORTED, platformManifest.support(de.t14d3.rapunzellib.events.block.BlockBreakSnapshot.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, platformManifest.support(InventoryActionPre.class).parity());
        assertEquals(GameEventSupportManifests.GUI_INVENTORY_BRIDGE_DETAILS, platformManifest.support(InventoryActionPre.class).details());
    }

    @Test
    void installRegistersManifestIntoContext(@TempDir Path dir) {
        TestContext context = new TestContext(serverRuntime(PlatformId.PAPER, RuntimeCapability.EVENTS), dir);
        Rapunzel.bootstrap(this, context);

        GameEventBus bus = GameEvents.install();

        assertSame(bus, context.services().get(GameEventBus.class));
        assertTrue(context.services().find(GameEventBridge.class).isPresent());
        assertEquals(1, TestGameEventBridgeInstaller.installCalls());

        GameEventSupportManifest manifest = context.services().get(GameEventSupportManifest.class);
        assertEquals(TestGameEventBridgeInstaller.MANIFEST.overlayUnsupported(TestInventoryGameEventSupportContributor.MANIFEST), manifest);
        assertSame(manifest, GameEvents.support());
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
