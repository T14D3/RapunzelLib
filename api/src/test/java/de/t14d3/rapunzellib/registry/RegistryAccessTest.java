package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.common.registry.DefaultRRegistryAccess;
import de.t14d3.rapunzellib.common.registry.RegistryAccessBackedBlockTypeRegistry;
import de.t14d3.rapunzellib.common.registry.RegistryAccessBackedEntityTypeRegistry;
import de.t14d3.rapunzellib.common.registry.RegistryAccessBackedItemTypeRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.RapunzelRuntime;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.registry.catalog.VanillaBlockTypes;
import de.t14d3.rapunzellib.registry.catalog.VanillaEntityTypes;
import de.t14d3.rapunzellib.registry.catalog.VanillaItemTypes;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RegistryAccessTest {
    @AfterEach
    void tearDown() {
        Rapunzel.shutdownAll();
    }

    @Test
    void genericRegistriesExposeLookupRefsAndEnumeration() {
        Object zombieHandle = new Object();
        Object skeletonHandle = new Object();
        TestEntityType zombie = new TestEntityType(RKey.of("minecraft:zombie"), zombieHandle);
        TestEntityType skeleton = new TestEntityType(RKey.of("minecraft:skeleton"), skeletonHandle);
        TestEntityTypeRegistry entityTypes = new TestEntityTypeRegistry(zombie, skeleton);
        DefaultRRegistryAccess registries = new DefaultRRegistryAccess();
        registries.register(RRegistries.ENTITY_TYPES, entityTypes);

        assertSame(entityTypes, registries.registry(RRegistries.ENTITY_TYPES));
        assertSame(zombie, registries.find(RRegistries.ENTITY_TYPES, zombie.key()).orElseThrow());
        assertSame(skeleton, RRegistries.ENTITY_TYPES.ref("minecraft:skeleton").require(registries));
        assertTrue(registries.registry(RRegistries.ENTITY_TYPES).contains(RKey.of("minecraft:skeleton")));
        assertEquals(List.of(RRegistries.ENTITY_TYPES), registries.registryKeys());
        assertEquals(List.of(RKey.of("minecraft:zombie"), RKey.of("minecraft:skeleton")), entityTypes.keys());
        assertEquals(List.of(
            RRegistries.ENTITY_TYPES.ref("minecraft:zombie"),
            RRegistries.ENTITY_TYPES.ref("minecraft:skeleton")
        ), entityTypes.refs());
        assertEquals(List.of(zombie, skeleton), entityTypes.entries());

        Object stoneItemHandle = new Object();
        Object appleHandle = new Object();
        TestItemType stone = new TestItemType(RKey.of("minecraft:stone"), stoneItemHandle);
        TestItemType apple = new TestItemType(RKey.of("minecraft:apple"), appleHandle);
        TestItemTypeRegistry itemTypes = new TestItemTypeRegistry(stone, apple);
        registries.register(RRegistries.ITEM_TYPES, itemTypes);

        assertSame(apple, registries.require(RRegistries.ITEM_TYPES.ref("minecraft:apple")));
        assertTrue(registries.registry(RRegistries.ITEM_TYPES).contains("minecraft:stone"));
        assertEquals(List.of(stone, apple), itemTypes.entries());

        Object oakLogHandle = new Object();
        Object stoneBlockHandle = new Object();
        TestBlockType oakLog = new TestBlockType(RKey.of("minecraft:oak_log"), oakLogHandle);
        TestBlockType stoneBlock = new TestBlockType(RKey.of("minecraft:stone"), stoneBlockHandle);
        TestBlockTypeRegistry blockTypes = new TestBlockTypeRegistry(oakLog, stoneBlock);
        registries.register(RRegistries.BLOCK_TYPES, blockTypes);

        assertSame(stoneBlock, registries.registry(RRegistries.BLOCK_TYPES).require("minecraft:stone"));
        assertTrue(blockTypes.contains("minecraft:oak_log"));
        assertEquals(List.of(oakLog, stoneBlock), blockTypes.entries());
        assertSame(stoneBlockHandle, stoneBlock.handle());
    }

    @Test
    void contextRapunzelAndRuntimeWrappersResolveRegisteredTypesThroughUnifiedAccess() {
        Object zombieHandle = new Object();
        Object appleHandle = new Object();
        Object stoneHandle = new Object();
        TestEntityType zombie = new TestEntityType(RKey.of("minecraft:zombie"), zombieHandle);
        TestItemType apple = new TestItemType(RKey.of("minecraft:apple"), appleHandle);
        TestBlockType stone = new TestBlockType(RKey.of("minecraft:stone"), stoneHandle);
        DefaultRRegistryAccess registries = new DefaultRRegistryAccess();
        registries.register(RRegistries.ENTITY_TYPES, new TestEntityTypeRegistry(zombie));
        registries.register(RRegistries.ITEM_TYPES, new TestItemTypeRegistry(apple));
        registries.register(RRegistries.BLOCK_TYPES, new TestBlockTypeRegistry(stone));
        TestContext context = new TestContext(Path.of("."));
        context.registerService(RRegistryAccess.class, registries);

        Rapunzel.bootstrap(this, context);

        // Register type registry wrappers in the global runtime (normally done by BootstrapServices).
        RapunzelRuntime.getInstance().registerIfAbsent(REntityTypeRegistry.class,
            new RegistryAccessBackedEntityTypeRegistry(Rapunzel.registries()));
        RapunzelRuntime.getInstance().registerIfAbsent(RItemTypeRegistry.class,
            new RegistryAccessBackedItemTypeRegistry(Rapunzel.registries()));
        RapunzelRuntime.getInstance().registerIfAbsent(RBlockTypeRegistry.class,
            new RegistryAccessBackedBlockTypeRegistry(Rapunzel.registries()));

        TestEntity entity = new TestEntity(UUID.fromString("00000000-0000-0000-0000-000000000123"), zombie.key());
        TestBlock block = new TestBlock(stone.key());
        TestBlockData blockData = new TestBlockData(stone.key());
        assertEquals(List.of(RRegistries.ENTITY_TYPES, RRegistries.ITEM_TYPES, RRegistries.BLOCK_TYPES), Rapunzel.registries().registryKeys());
        assertInstanceOf(REntityTypeRegistry.class, Rapunzel.context().entityTypes());
        assertSame(zombie, Rapunzel.context().entityTypes().require(zombie.key()));
        assertSame(zombie, Rapunzel.entityTypes().require("minecraft:zombie"));
        assertSame(zombie, RRegistries.ENTITY_TYPES.ref(zombie.key()).require());
        assertSame(zombie, REntityType.require("minecraft:zombie"));
        assertEquals(RRegistries.ENTITY_TYPES.ref(zombie.key()), entity.typeRef());
        assertSame(zombie, entity.type().orElseThrow());
        assertSame(zombie, entity.requireType());
        assertSame(apple, Rapunzel.itemTypes().require("minecraft:apple"));
        assertSame(apple, RItemType.require("minecraft:apple"));
        assertSame(stone, Rapunzel.context().blockTypes().require(stone.key()));
        assertSame(stone, Rapunzel.context().blockTypes().find(stone.key()).orElseThrow());
        assertSame(stone, Rapunzel.blockTypes().require("minecraft:stone"));
        assertSame(stone, RBlockType.find("minecraft:stone").orElseThrow());
        assertSame(stone, RBlockType.require("minecraft:stone"));
        assertEquals(RRegistries.BLOCK_TYPES.ref(stone.key()), block.typeRef());
        assertSame(stone, block.type().orElseThrow());
        assertSame(stone, block.requireType());
        assertEquals(RRegistries.BLOCK_TYPES.ref(stone.key()), blockData.typeRef());
        assertSame(stone, blockData.type().orElseThrow());
        assertSame(stone, blockData.requireType());
        assertSame(stoneHandle, RBlockType.require("minecraft:stone").handle());
    }

    @Test
    void generatedVanillaCatalogsExposeTypedKeysAndRefs() {
        TestEntityType zombie = new TestEntityType(VanillaEntityTypes.Minecraft.ZOMBIE_KEY, new Object());
        TestItemType stoneItem = new TestItemType(VanillaItemTypes.Minecraft.STONE_KEY, new Object());
        TestBlockType stoneBlock = new TestBlockType(VanillaBlockTypes.Minecraft.STONE_KEY, new Object());

        DefaultRRegistryAccess registries = new DefaultRRegistryAccess();
        registries.register(RRegistries.ENTITY_TYPES, new TestEntityTypeRegistry(zombie));
        registries.register(RRegistries.ITEM_TYPES, new TestItemTypeRegistry(stoneItem));
        registries.register(RRegistries.BLOCK_TYPES, new TestBlockTypeRegistry(stoneBlock));

        TestContext context = new TestContext(Path.of("."));
        context.registerService(RRegistryAccess.class, registries);
        Rapunzel.bootstrap(this, context);

        assertEquals(RKey.of("minecraft:zombie"), VanillaEntityTypes.Minecraft.ZOMBIE_KEY);
        assertEquals(RRegistries.ENTITY_TYPES, VanillaEntityTypes.REGISTRY);
        assertEquals(RRegistries.ENTITY_TYPES.ref("minecraft:zombie"), VanillaEntityTypes.Minecraft.ZOMBIE);
        assertSame(zombie, VanillaEntityTypes.Minecraft.ZOMBIE.require());

        assertEquals(RKey.of("minecraft:stone"), VanillaItemTypes.Minecraft.STONE_KEY);
        assertEquals(RRegistries.ITEM_TYPES, VanillaItemTypes.REGISTRY);
        assertEquals(RRegistries.ITEM_TYPES.ref("minecraft:stone"), VanillaItemTypes.Minecraft.STONE);
        assertSame(stoneItem, VanillaItemTypes.Minecraft.STONE.require());
        assertSame(stoneItem, VanillaItemTypes.require("minecraft:stone"));

        assertEquals(RKey.of("minecraft:stone"), VanillaBlockTypes.Minecraft.STONE_KEY);
        assertEquals(RRegistries.BLOCK_TYPES, VanillaBlockTypes.REGISTRY);
        assertEquals(RRegistries.BLOCK_TYPES.ref("minecraft:stone"), VanillaBlockTypes.Minecraft.STONE);
        assertSame(stoneBlock, VanillaBlockTypes.Minecraft.STONE.require());
        assertSame(stoneBlock, VanillaBlockTypes.require(VanillaBlockTypes.Minecraft.STONE_KEY));
    }

    private static final class TestEntityType extends RRegistryTypeHandle<Object> implements REntityType {
        private TestEntityType(@NotNull RKey key, @NotNull Object handle) {
            super(PlatformId.PAPER, key, handle);
        }
    }

    private static final class TestItemType extends RRegistryTypeHandle<Object> implements RItemType {
        private TestItemType(@NotNull RKey key, @NotNull Object handle) {
            super(PlatformId.PAPER, key, handle);
        }
    }

    private static final class TestBlockType extends RRegistryTypeHandle<Object> implements RBlockType {
        private TestBlockType(@NotNull RKey key, @NotNull Object handle) {
            super(PlatformId.PAPER, key, handle);
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

    private static final class TestEntity extends RNativeHandle<Object> implements REntity {
        private final UUID uuid;
        private final RKey typeKey;

        private TestEntity(@NotNull UUID uuid, @NotNull RKey typeKey) {
            super(PlatformId.PAPER, new Object());
            this.uuid = uuid;
            this.typeKey = typeKey;
        }

        @Override
        public @NotNull UUID uuid() {
            return uuid;
        }

        @Override
        public @NotNull RRegistryRef<REntityType> typeRef() {
            return REntityType.ref(typeKey);
        }

        @Override
        public @NotNull Optional<RWorld> world() {
            return Optional.of(new TestWorld());
        }

        @Override
        public @NotNull Optional<RLocation> location() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<String> getName() {
            return Optional.empty();
        }

        @Override
        public void setName(@NotNull String name) {
        }

        @Override
        public @NotNull Optional<Component> getDisplayName() {
            return Optional.empty();
        }

        @Override
        public void setDisplayName(@NotNull Component displayName) {
        }

        @Override
        public boolean remove() {
            return false;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }
    }

    private static final class TestWorld extends RNativeHandle<Object> implements RWorld {
        private TestWorld() {
            super(PlatformId.PAPER, new Object());
        }

        @Override
        public @NotNull RWorldRef ref() {
            return new RWorldRef("world", "minecraft:test");
        }
    }

    private static final class TestBlock extends RNativeHandle<Object> implements RBlock {
        private final RKey typeKey;

        private TestBlock(@NotNull RKey typeKey) {
            super(PlatformId.PAPER, new Object());
            this.typeKey = typeKey;
        }

        @Override
        public @NotNull RWorld world() {
            return new TestWorld();
        }

        @Override
        public @NotNull de.t14d3.rapunzellib.objects.RBlockPos pos() {
            return new de.t14d3.rapunzellib.objects.RBlockPos(1, 2, 3);
        }

        @Override
        public @NotNull RRegistryRef<RBlockType> typeRef() {
            return RBlockType.ref(typeKey);
        }

        @Override
        public @NotNull RBlockData data() {
            return new TestBlockData(typeKey);
        }
    }

    private static final class TestBlockData extends RNativeHandle<Object> implements RBlockData {
        private final RKey typeKey;

        private TestBlockData(@NotNull RKey typeKey) {
            super(PlatformId.PAPER, new Object());
            this.typeKey = typeKey;
        }

        @Override
        public @NotNull RRegistryRef<RBlockType> typeRef() {
            return RBlockType.ref(typeKey);
        }

        @Override
        public @NotNull String asString() {
            return typeKey.asString();
        }
    }

    private static final class TestContext implements RapunzelContext {
        private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

        private final Path dataDir;
        private final ServiceRegistry services = new MapServiceRegistry();
        private final Scheduler scheduler = new InlineScheduler();
        private final PlatformRuntime runtime = new PlatformRuntime(
            PlatformId.PAPER,
            RuntimeRole.SERVER,
            EngineFamily.MOJANG_SERVER,
            EnumSet.of(RuntimeCapability.ENTITIES, RuntimeCapability.INVENTORY, RuntimeCapability.BLOCKS),
            new LifecycleOwner(this)
        );

        private TestContext(Path dataDir) {
            this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
        }

        private <T> void registerService(@NotNull Class<T> type, @NotNull T instance) {
            services.register(type, instance);
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
