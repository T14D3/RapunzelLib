package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class RItemTypeLookupTest {
    @AfterEach
    void tearDown() {
        Rapunzel.shutdownAll();
    }

    @Test
    void itemsResolveRuntimeItemTypesFromRegisteredRegistry() {
        TestItemType stone = new TestItemType(RKey.of("minecraft:stone"), "stone-handle");
        TestContext context = new TestContext(Path.of("."));
        context.services().register(RItemTypeRegistry.class, new TestItemTypeRegistry(stone));
        Rapunzel.bootstrap(this, context);

        RItem item = RItem.of("minecraft:stone", 2);

        assertEquals(RItemType.ref("minecraft:stone"), item.typeRef());
        assertSame(stone, item.type().orElseThrow());
        assertSame(stone, item.requireType());
    }

    private static final class TestItemType extends RRegistryTypeHandle<Object> implements RItemType {
        private TestItemType(@NotNull RKey key, @NotNull Object handle) {
            super(PlatformId.PAPER, key, handle);
        }
    }

    private static final class TestItemTypeRegistry implements RItemTypeRegistry {
        private final TestItemType itemType;

        private TestItemTypeRegistry(TestItemType itemType) {
            this.itemType = itemType;
        }

        @Override
        public @NotNull Optional<RItemType> find(@NotNull RKey key) {
            return itemType.key().equals(key) ? Optional.of(itemType) : Optional.empty();
        }

        @Override
        public @NotNull List<RItemType> entries() {
            return List.of(itemType);
        }
    }

    private static final class TestContext implements RapunzelContext {
        private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

        private final Path dataDir;
        private final MapServiceRegistry services = new MapServiceRegistry();
        private final Scheduler scheduler = new InlineScheduler();
        private final PlatformRuntime runtime = new PlatformRuntime(
            PlatformId.PAPER,
            RuntimeRole.SERVER,
            EngineFamily.MOJANG_SERVER,
            EnumSet.of(RuntimeCapability.INVENTORY),
            new LifecycleOwner(this)
        );

        private TestContext(Path dataDir) {
            this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
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
            return instance == null ? Optional.empty() : Optional.of(type.cast(instance));
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
