package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.objects.snapshot.RBlockSnapshot;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LiveSnapshotAndActionContractsTest {
    @Test
    void entitySnapshotsFreezeLiveCoordinatesWhileLiveActionsRemainMutable() {
        TestWorld world = new TestWorld();
        MutableLivingEntity entity = new MutableLivingEntity(world, new RLocation(world.ref(), 3.8, 70.0, 2.2, 45.0f, 10.0f));

        REntitySnapshot snapshot = entity.snapshot();
        entity.moveTo(new RLocation(world.ref(), 9.0, 80.0, -1.0, 90.0f, 20.0f));

        assertEquals(new RBlockPos(3, 70, 2), snapshot.pos());
        assertEquals(RKey.of("test:entity"), snapshot.entityTypeKey());
        assertTrue(entity.teleport(new RLocation(world.ref(), 1.0, 65.0, 1.0, 0.0f, 0.0f)));
        assertTrue(entity.damage(2.0d));
        assertTrue(entity.heal(1.0d));
        assertEquals(9.0d, entity.health());
    }

    @Test
    void blockSnapshotsAndActionsExposeImmutableStateAndContainerLookup() {
        TestWorld world = new TestWorld();
        TestInventory inventory = new TestInventory();
        MutableBlock block = new MutableBlock(world, new TestBlockData("test:chest[state=open]"), inventory);

        RBlockSnapshot snapshot = block.snapshot();
        assertTrue(block.setData(new TestBlockData("test:barrel[facing=north]")));

        assertEquals("test:chest[state=open]", snapshot.dataString());
        assertEquals(RKey.of("test:chest"), snapshot.blockTypeKey());
        assertSame(inventory, block.container().orElseThrow());
        assertSame(inventory, block.container(TestInventory.class).orElseThrow());
        assertSame(inventory, block.requireContainer(TestInventory.class));
    }

    @Test
    void worldsCanSpawnLiveEntities() {
        TestWorld world = new TestWorld();
        RLocation spawn = new RLocation(world.ref(), 2.0, 65.0, 2.0, 180.0f, 0.0f);

        MutableLivingEntity entity = (MutableLivingEntity) world.spawn(REntityType.ref("test:entity"), spawn).orElseThrow();

        assertEquals(spawn, entity.location().orElseThrow());
        assertEquals(world, entity.world().orElseThrow());
    }

    private static final class TestWorld extends RNativeHandle<Object> implements RWorld {
        private TestWorld() {
            super(PlatformId.PAPER, new Object());
        }

        @Override
        public @NotNull RWorldRef ref() {
            return new RWorldRef("test-world", "test:world");
        }

        @Override
        public boolean canSpawnEntities() {
            return true;
        }

        @Override
        public @NotNull Optional<REntity> spawn(@NotNull RRegistryRef<REntityType> type, @NotNull RLocation location) {
            return Optional.of(new MutableLivingEntity(this, location, type.key()));
        }
    }

    private static final class MutableLivingEntity extends RNativeHandle<Object> implements RLivingEntity {
        private final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000929");
        private final TestWorld world;
        private final RKey typeKey;
        private RLocation location;
        private double health = 10.0d;

        private MutableLivingEntity(TestWorld world, RLocation location) {
            this(world, location, RKey.of("test:entity"));
        }

        private MutableLivingEntity(TestWorld world, RLocation location, RKey typeKey) {
            super(PlatformId.PAPER, new Object());
            this.world = world;
            this.location = location;
            this.typeKey = typeKey;
        }

        private void moveTo(RLocation location) {
            this.location = location;
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
            return Optional.of(world);
        }

        @Override
        public @NotNull Optional<RLocation> location() {
            return Optional.of(location);
        }

        @Override
        public boolean canTeleport() {
            return true;
        }

        @Override
        public boolean teleport(@NotNull RLocation location) {
            this.location = location;
            return true;
        }

        @Override
        public double health() {
            return health;
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
            return health > 0.0d;
        }

        @Override
        public boolean canDamage() {
            return true;
        }

        @Override
        public boolean damage(double amount) {
            health = Math.max(0.0d, health - amount);
            return true;
        }

        @Override
        public boolean canHeal() {
            return true;
        }

        @Override
        public boolean heal(double amount) {
            health = Math.min(maxHealth(), health + amount);
            return true;
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

    private static final class MutableBlock extends RNativeHandle<Object> implements RBlock {
        private final TestWorld world;
        private final RBlockPos pos = new RBlockPos(4, 65, 4);
        private final TestInventory inventory;
        private RBlockData data;

        private MutableBlock(TestWorld world, RBlockData data, TestInventory inventory) {
            super(PlatformId.PAPER, new Object());
            this.world = world;
            this.data = data;
            this.inventory = inventory;
        }

        @Override
        public @NotNull RWorld world() {
            return world;
        }

        @Override
        public @NotNull RBlockPos pos() {
            return pos;
        }

        @Override
        public @NotNull RRegistryRef<RBlockType> typeRef() {
            return data.typeRef();
        }

        @Override
        public @NotNull RBlockData data() {
            return data;
        }

        @Override
        public boolean canSetData() {
            return true;
        }

        @Override
        public boolean setData(@NotNull RBlockData data) {
            this.data = data;
            return true;
        }

        @Override
        public @NotNull Optional<RContainer> container() {
            return Optional.of(inventory);
        }
    }

    private static final class TestBlockData extends RNativeHandle<Object> implements RBlockData {
        private final String value;

        private TestBlockData(String value) {
            super(PlatformId.PAPER, new Object());
            this.value = value;
        }

        @Override
        public @NotNull RRegistryRef<RBlockType> typeRef() {
            String id = value.contains("[") ? value.substring(0, value.indexOf('[')) : value;
            return RBlockType.ref(id);
        }

        @Override
        public @NotNull String asString() {
            return value;
        }
    }

    private static final class TestInventory extends RNativeHandle<Object> implements RContainer {
        private TestInventory() {
            super(PlatformId.PAPER, new Object());
        }
    }
}
