package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.inventory.Inventories;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InventoryEventBridgeTest {
    @AfterEach
    void tearDown() {
        Rapunzel.shutdownAll();
    }

    @Test
    void dispatchOpenPrePublishesWrappedInventoryAndPlayerPayloads(@TempDir Path dir) {
        Object nativeInventory = new Object();
        TestInventory inventory = new TestInventory(nativeInventory, 3);
        TestPlayer player = new TestPlayer();
        TestContext context = new TestContext(dir);
        GameEventBus bus = new GameEventBus(new InlineScheduler(), LoggerFactory.getLogger(InventoryEventBridgeTest.class));

        context.services().register(Inventories.class, new TestInventories(nativeInventory, inventory));
        context.services().register(GameEventBus.class, bus);
        Rapunzel.bootstrap(this, context);

        AtomicReference<InventoryOpenPre> seen = new AtomicReference<>();
        bus.onPre(InventoryOpenPre.class, seen::set);

        assertTrue(InventoryEventBridge.dispatchOpenPre(player, inventory));

        InventoryOpenPre event = seen.get();
        assertSame(player, event.player());
        assertSame(player.handle(), event.player().handle());
        assertSame(inventory, event.inventory());
        assertSame(nativeInventory, event.inventory().handle());
    }

    @Test
    void dispatchOpenPreReturnsFalseWhenListenerDenies(@TempDir Path dir) {
        TestInventory inventory = new TestInventory(new Object(), 1);
        TestPlayer player = new TestPlayer();
        TestContext context = new TestContext(dir);
        GameEventBus bus = new GameEventBus(new InlineScheduler(), LoggerFactory.getLogger(InventoryEventBridgeTest.class));

        context.services().register(Inventories.class, new TestInventories(inventory.handle(), inventory));
        context.services().register(GameEventBus.class, bus);
        Rapunzel.bootstrap(this, context);

        AtomicReference<InventoryOpenPre> seen = new AtomicReference<>();
        bus.onPre(InventoryOpenPre.class, event -> {
            seen.set(event);
            event.deny();
        });

        assertFalse(InventoryEventBridge.dispatchOpenPre(player, inventory));
        assertTrue(seen.get().isDenied());
    }

    private static final class TestContext implements RapunzelContext {
        private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

        private final Path dataDirectory;
        private final ServiceRegistry services = new MapServiceRegistry();
        private final Scheduler scheduler = new InlineScheduler();

        private TestContext(Path dataDirectory) {
            this.dataDirectory = dataDirectory;
        }

        @Override
        public @NotNull PlatformRuntime runtime() {
            return new PlatformRuntime(
                PlatformId.PAPER,
                RuntimeRole.SERVER,
                EngineFamily.MOJANG_SERVER,
                java.util.EnumSet.noneOf(de.t14d3.rapunzellib.runtime.RuntimeCapability.class),
                new LifecycleOwner(new Object())
            );
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

    private static final class TestInventories implements Inventories {
        private final Object nativeInventory;
        private final RInventory inventory;

        private TestInventories(@NotNull Object nativeInventory, @NotNull RInventory inventory) {
            this.nativeInventory = nativeInventory;
            this.inventory = inventory;
        }

        @Override
        public @NotNull PlatformId platformId() {
            return PlatformId.PAPER;
        }

        @Override
        public @NotNull Optional<RInventory> wrap(@Nullable Object nativeInventory) {
            if (nativeInventory == this.nativeInventory) {
                return Optional.of(inventory);
            }
            return Optional.empty();
        }
    }

    private static final class TestInventory extends RNativeHandle<Object> implements RInventory {
        private final RItem[] items;

        private TestInventory(@NotNull Object handle, int size) {
            super(PlatformId.PAPER, handle);
            this.items = new RItem[size];
        }

        @Override
        public int size() {
            return items.length;
        }

        @Override
        public @NotNull Optional<RItem> item(int slot) {
            return Optional.ofNullable(items[slot]);
        }

        @Override
        public void setItem(int slot, @Nullable RItem item) {
            items[slot] = item;
        }
    }

    private static final class TestPlayer extends RNativeHandle<Object> implements RServerPlayer {
        private TestPlayer() {
            super(PlatformId.PAPER, new Object());
        }

        @Override
        public @NotNull Audience audience() {
            return Audience.empty();
        }

        @Override
        public @NotNull UUID uuid() {
            return UUID.fromString("00000000-0000-0000-0000-00000000babe");
        }

        @Override
        public @NotNull String name() {
            return "player";
        }

        @Override
        public boolean hasPermission(@NotNull String permission) {
            return true;
        }

        @Override
        public @NotNull Optional<RWorld> world() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<RLocation> location() {
            return Optional.empty();
        }

        @Override
        public double health() {
            return 20.0d;
        }

        @Override
        public double maxHealth() {
            return 20.0d;
        }

        @Override
        public int remainingAir() {
            return 300;
        }

        @Override
        public int maxAir() {
            return 300;
        }

        @Override
        public boolean isAlive() {
            return true;
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
