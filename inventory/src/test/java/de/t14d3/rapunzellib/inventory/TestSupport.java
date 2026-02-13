package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class TestSupport {
    private TestSupport() {
    }

    static PlatformRuntime serverRuntime(PlatformId platformId, RuntimeCapability... capabilities) {
        return runtime(platformId, RuntimeRole.SERVER, EngineFamily.MOJANG_SERVER, capabilities);
    }

    static PlatformRuntime proxyRuntime(PlatformId platformId, RuntimeCapability... capabilities) {
        return runtime(platformId, RuntimeRole.PROXY, EngineFamily.PROXY, capabilities);
    }

    static PlatformRuntime runtime(
        PlatformId platformId,
        RuntimeRole role,
        EngineFamily engineFamily,
        RuntimeCapability... capabilities
    ) {
        EnumSet<RuntimeCapability> capabilitySet = EnumSet.noneOf(RuntimeCapability.class);
        Collections.addAll(capabilitySet, capabilities);
        return new PlatformRuntime(platformId, role, engineFamily, capabilitySet, new LifecycleOwner(new Object()));
    }

    static InventoryWrapperFactory<TestNativeInventory> testFactory(PlatformId platformId) {
        return InventoryFeatureInstallerSupport.slotInventoryFactory(
            platformId,
            InventoryFeatureInstallerSupport.SlotInventoryAdapter.<TestNativeInventory, TestNativeItem>builder(
                TestNativeInventory.class,
                new TestItemStackAdapter()
            )
                .size(TestNativeInventory::size)
                .getItem(TestNativeInventory::item)
                .setItem(TestNativeInventory::set)
                .clear(TestNativeInventory::clear)
                .isEmptyItem(TestNativeItem::isEmpty)
                .emptyItem(TestNativeItem::empty)
                .build()
        );
    }

    static final class TestContext implements RapunzelContext {
        private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

        private final PlatformRuntime runtime;
        private final Path dataDirectory;
        private final ServiceRegistry services = new MapServiceRegistry();
        private final Scheduler scheduler = new InlineScheduler();

        TestContext(PlatformRuntime runtime, Path dataDirectory) {
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

    static final class TestNativeInventory {
        private final List<TestNativeItem> slots;

        TestNativeInventory(int size) {
            this.slots = new ArrayList<>(size);
            for (int slot = 0; slot < size; slot++) {
                this.slots.add(TestNativeItem.empty());
            }
        }

        int size() {
            return slots.size();
        }

        TestNativeItem item(int slot) {
            return slots.get(slot);
        }

        void set(int slot, @Nullable TestNativeItem item) {
            slots.set(slot, item == null ? TestNativeItem.empty() : item);
        }

        void clear() {
            for (int slot = 0; slot < slots.size(); slot++) {
                slots.set(slot, TestNativeItem.empty());
            }
        }
    }

    record TestNativeItem(String material, int amount) {
        static TestNativeItem empty() {
            return new TestNativeItem("minecraft:air", 0);
        }

        boolean isEmpty() {
            return amount <= 0 || material.equalsIgnoreCase("air") || material.endsWith(":air");
        }
    }

    static final class TestItemStackAdapter implements ItemStackAdapter<TestNativeItem> {
        @Override
        public @NotNull RItem snapshot(@NotNull TestNativeItem nativeItem) {
            return RItem.of(nativeItem.material(), nativeItem.amount());
        }

        @Override
        public @NotNull TestNativeItem create(@NotNull RItem item) {
            return new TestNativeItem(item.typeKey().asString(), item.amount());
        }

        @Override
        public @NotNull TestNativeItem apply(@NotNull TestNativeItem nativeItem, @NotNull RItem item) {
            return create(item);
        }

        @Override
        public boolean supports(@Nullable Object object) {
            return object instanceof TestNativeItem;
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
