package de.t14d3.rapunzellib.common.bootstrap;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.common.registry.DefaultRRegistryAccess;
import de.t14d3.rapunzellib.common.registry.RegistryAccessBackedBlockTypeRegistry;
import de.t14d3.rapunzellib.common.registry.RegistryAccessBackedEntityTypeRegistry;
import de.t14d3.rapunzellib.common.registry.RegistryAccessBackedItemTypeRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.objects.Entities;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.REntityTypeRegistry;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistries;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.registry.RRegistryKey;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

final class BootstrapServicesRegistryTest {
    @BeforeEach
    void resetRuntime() throws Exception {
        Rapunzel.sharedRuntime().shutdown();
    }

    @Test
    void registerTypeRegistriesWiresUnifiedAccessAndCompatibilityFacades() {
        RapunzelContext context = BootstrapServices.createContext(
            BootstrapServices.serverRuntime(
                PlatformId.PAPER,
                EngineFamily.MOJANG_SERVER,
                this,
                RuntimeCapability.ENTITIES,
                RuntimeCapability.INVENTORY,
                RuntimeCapability.BLOCKS
            ),
            LoggerFactory.getLogger(BootstrapServicesRegistryTest.class),
            Path.of("."),
            _path -> Optional.empty(),
            InlineScheduler.INSTANCE
        );

        TestEntityType zombie = new TestEntityType(RKey.of("minecraft:zombie"));
        TestItemType apple = new TestItemType(RKey.of("minecraft:apple"));
        TestBlockType stone = new TestBlockType(RKey.of("minecraft:stone"));
        TestEntityTypeRegistry entityTypes = new TestEntityTypeRegistry(zombie);
        TestItemTypeRegistry itemTypes = new TestItemTypeRegistry(apple);
        TestBlockTypeRegistry blockTypes = new TestBlockTypeRegistry(stone);

        BootstrapServices.registerTypeRegistries(context, entityTypes, itemTypes, blockTypes);

        RRegistryAccess registries = context.registries();
        assertInstanceOf(DefaultRRegistryAccess.class, registries);
        assertSame(registries, context.services().get(DefaultRRegistryAccess.class));
        assertEquals(List.of(RRegistries.ENTITY_TYPES, RRegistries.ITEM_TYPES, RRegistries.BLOCK_TYPES), registries.registryKeys());

        assertSame(entityTypes, registries.registry(RRegistries.ENTITY_TYPES));
        assertSame(itemTypes, registries.registry(RRegistries.ITEM_TYPES));
        assertSame(blockTypes, registries.registry(RRegistries.BLOCK_TYPES));

        assertInstanceOf(RegistryAccessBackedEntityTypeRegistry.class, context.sharedRuntime().get(REntityTypeRegistry.class));
        assertInstanceOf(RegistryAccessBackedItemTypeRegistry.class, context.sharedRuntime().get(RItemTypeRegistry.class));
        assertInstanceOf(RegistryAccessBackedBlockTypeRegistry.class, context.sharedRuntime().get(RBlockTypeRegistry.class));
        assertNotSame(entityTypes, context.entityTypes());
        assertNotSame(itemTypes, context.itemTypes());
        assertNotSame(blockTypes, context.blockTypes());

        assertSame(zombie, context.entityTypes().require(zombie.key()));
        assertSame(apple, context.itemTypes().require(apple.key()));
        assertSame(stone, context.blockTypes().require(stone.key()));
    }

    @Test
    void registerRegistryAccessWiresCustomBridgeAndTypedCompatibilityServices() {
        RapunzelContext context = BootstrapServices.createContext(
            BootstrapServices.serverRuntime(
                PlatformId.PAPER,
                EngineFamily.MOJANG_SERVER,
                this,
                RuntimeCapability.ENTITIES,
                RuntimeCapability.INVENTORY,
                RuntimeCapability.BLOCKS
            ),
            LoggerFactory.getLogger(BootstrapServicesRegistryTest.class),
            Path.of("."),
            _path -> Optional.empty(),
            InlineScheduler.INSTANCE
        );

        TestEntityType zombie = new TestEntityType(RKey.of("minecraft:zombie"));
        TestItemType apple = new TestItemType(RKey.of("minecraft:apple"));
        TestBlockType stone = new TestBlockType(RKey.of("minecraft:stone"));
        BridgedRegistryAccess registries = new BridgedRegistryAccess(
            new TestEntityTypeRegistry(zombie),
            new TestItemTypeRegistry(apple),
            new TestBlockTypeRegistry(stone)
        );

        BootstrapServices.registerRegistryAccess(context, BridgedRegistryAccess.class, registries);

        assertSame(registries, context.registries());
        assertSame(registries, context.services().get(BridgedRegistryAccess.class));
        assertEquals(List.of(RRegistries.ENTITY_TYPES, RRegistries.ITEM_TYPES, RRegistries.BLOCK_TYPES), registries.registryKeys());
        assertInstanceOf(RegistryAccessBackedEntityTypeRegistry.class, context.sharedRuntime().get(REntityTypeRegistry.class));
        assertInstanceOf(RegistryAccessBackedItemTypeRegistry.class, context.sharedRuntime().get(RItemTypeRegistry.class));
        assertInstanceOf(RegistryAccessBackedBlockTypeRegistry.class, context.sharedRuntime().get(RBlockTypeRegistry.class));
        assertSame(zombie, context.entityTypes().require(zombie.key()));
        assertSame(apple, context.itemTypes().require(apple.key()));
        assertSame(stone, context.blockTypes().require(stone.key()));
    }

    @Test
    void registerServerPlatformServicesWiresAccessorsNativeInteropAndRegistries() {
        RapunzelContext context = BootstrapServices.createContext(
            BootstrapServices.serverRuntime(
                PlatformId.PAPER,
                EngineFamily.MOJANG_SERVER,
                this,
                RuntimeCapability.ENTITIES,
                RuntimeCapability.INVENTORY,
                RuntimeCapability.BLOCKS,
                RuntimeCapability.WORLDS
            ),
            LoggerFactory.getLogger(BootstrapServicesRegistryTest.class),
            Path.of("."),
            _path -> Optional.empty(),
            InlineScheduler.INSTANCE
        );
        BootstrapServices.registerNativeInterop(context);

        TestPlayers players = new TestPlayers();
        TestEntities entities = new TestEntities();
        TestWorlds worlds = new TestWorlds();
        TestBlocks blocks = new TestBlocks();
        BridgedRegistryAccess registries = new BridgedRegistryAccess(
            new TestEntityTypeRegistry(new TestEntityType(RKey.of("minecraft:zombie"))),
            new TestItemTypeRegistry(new TestItemType(RKey.of("minecraft:apple"))),
            new TestBlockTypeRegistry(new TestBlockType(RKey.of("minecraft:stone")))
        );
        AtomicReference<MutableRNativeInterop> nativeInterop = new AtomicReference<>();

        BootstrapServices.registerServerPlatformServices(
            context,
            players,
            TestPlayers.class,
            entities,
            TestEntities.class,
            worlds,
            TestWorlds.class,
            blocks,
            TestBlocks.class,
            nativeInterop::set,
            () -> registries
        );

        assertSame(players, context.players());
        assertSame(entities, context.entities());
        assertSame(worlds, context.worlds());
        assertSame(blocks, context.blocks());
        assertSame(context.services().get(MutableRNativeInterop.class), nativeInterop.get());
        assertSame(registries, context.registries());
    }

    private static final class TestEntityType extends RRegistryTypeHandle<Object> implements REntityType {
        private TestEntityType(@NotNull RKey key) {
            super(PlatformId.PAPER, key, new Object());
        }
    }

    private static final class TestItemType extends RRegistryTypeHandle<Object> implements RItemType {
        private TestItemType(@NotNull RKey key) {
            super(PlatformId.PAPER, key, new Object());
        }
    }

    private static final class TestBlockType extends RRegistryTypeHandle<Object> implements RBlockType {
        private TestBlockType(@NotNull RKey key) {
            super(PlatformId.PAPER, key, new Object());
        }
    }

    private static final class TestEntityTypeRegistry implements REntityTypeRegistry {
        private final Map<RKey, REntityType> entries = new LinkedHashMap<>();

        private TestEntityTypeRegistry(REntityType... entries) {
            for (REntityType entry : entries) {
                this.entries.put(entry.key(), entry);
            }
        }

        @Override
        public @NotNull Optional<REntityType> find(@NotNull RKey key) {
            return Optional.ofNullable(entries.get(key));
        }

        @Override
        public @NotNull List<REntityType> entries() {
            return List.copyOf(entries.values());
        }
    }

    private static final class TestItemTypeRegistry implements RItemTypeRegistry {
        private final Map<RKey, RItemType> entries = new LinkedHashMap<>();

        private TestItemTypeRegistry(RItemType... entries) {
            for (RItemType entry : entries) {
                this.entries.put(entry.key(), entry);
            }
        }

        @Override
        public @NotNull Optional<RItemType> find(@NotNull RKey key) {
            return Optional.ofNullable(entries.get(key));
        }

        @Override
        public @NotNull List<RItemType> entries() {
            return List.copyOf(entries.values());
        }
    }

    private static final class TestBlockTypeRegistry implements RBlockTypeRegistry {
        private final Map<RKey, RBlockType> entries = new LinkedHashMap<>();

        private TestBlockTypeRegistry(RBlockType... entries) {
            for (RBlockType entry : entries) {
                this.entries.put(entry.key(), entry);
            }
        }

        @Override
        public @NotNull Optional<RBlockType> find(@NotNull RKey key) {
            return Optional.ofNullable(entries.get(key));
        }

        @Override
        public @NotNull List<RBlockType> entries() {
            return List.copyOf(entries.values());
        }
    }

    private static final class BridgedRegistryAccess implements RRegistryAccess {
        private final DefaultRRegistryAccess registries = new DefaultRRegistryAccess();

        private BridgedRegistryAccess(
            @NotNull REntityTypeRegistry entityTypes,
            @NotNull RItemTypeRegistry itemTypes,
            @NotNull RBlockTypeRegistry blockTypes
        ) {
            registries.register(RRegistries.ENTITY_TYPES, entityTypes);
            registries.register(RRegistries.ITEM_TYPES, itemTypes);
            registries.register(RRegistries.BLOCK_TYPES, blockTypes);
        }

        @Override
        public <T> @NotNull Optional<de.t14d3.rapunzellib.registry.RRegistry<T>> findRegistry(@NotNull RRegistryKey<T> registryKey) {
            return registries.findRegistry(registryKey);
        }

        @Override
        public @NotNull List<RRegistryKey<?>> registryKeys() {
            return registries.registryKeys();
        }
    }

    private static final class TestPlayers implements Players {
        @Override
        public @NotNull Collection<RPlayer> online() {
            return List.of();
        }

        @Override
        public @NotNull Optional<RPlayer> get(@NotNull UUID uuid) {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<RPlayer> wrap(@NotNull Object nativePlayer) {
            return Optional.empty();
        }
    }

    private static final class TestEntities implements Entities {
        @Override
        public @NotNull Optional<REntity> get(@NotNull UUID uuid) {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<REntity> wrap(@NotNull Object nativeEntity) {
            return Optional.empty();
        }
    }

    private static final class TestWorlds implements Worlds {
        @Override
        public @NotNull Collection<RWorld> all() {
            return List.of();
        }

        @Override
        public @NotNull Optional<RWorld> getByName(@NotNull String name) {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<RWorld> get(@NotNull RKey key) {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld) {
            return Optional.empty();
        }
    }

    private static final class TestBlocks implements Blocks {
        @Override
        public @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock) {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<RBlockData> wrapData(@NotNull Object nativeBlockData) {
            return Optional.empty();
        }

        @Override
        public @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull Optional<RBlockData> parseData(@NotNull String value) {
            return Optional.empty();
        }
    }

    private enum InlineScheduler implements Scheduler {
        INSTANCE;

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
