package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LiveEntityPayloadsTest {
    @Test
    void interactAndAttackEventsExposeLiveEntityAlongsideCapturedMetadata() {
        MutableLivingEntity entity = new MutableLivingEntity("test:zombie", new RLocation(TestWorld.REF, 4.2, 65.0, 7.9, 0.0f, 0.0f));
        TestServerPlayer player = new TestServerPlayer();

        InteractEntityPre interact = new InteractEntityPre(player, entity, true);
        AttackEntityPost attack = new AttackEntityPost(player, entity, false);

        entity.moveTo(new RLocation(TestWorld.REF, 10.0, 80.0, -3.0, 0.0f, 0.0f));

        assertSame(entity, interact.entity());
        assertSame(entity, attack.entity());
        assertSame(entity, interact.livingEntity().orElseThrow());
        assertSame(entity, attack.livingEntity().orElseThrow());
        assertEquals(new RBlockPos(4, 65, 7), interact.pos());
        assertEquals(new RBlockPos(4, 65, 7), attack.pos());
        assertEquals(TestWorld.REF, interact.world());
        assertEquals(RKey.of("test:zombie"), attack.entityTypeKey());
    }

    @Test
    void hurtAndSpawnPostEventsCaptureMetadataFromLiveEntity() {
        MutableLivingEntity entity = new MutableLivingEntity("test:skeleton", new RLocation(TestWorld.REF, 1.1, 70.0, 2.9, 0.0f, 0.0f));

        EntityHurtPre hurt = new EntityHurtPre(entity, "test:fire", false);
        EntitySpawnPost spawn = new EntitySpawnPost(entity, "NATURAL", false);

        entity.moveTo(new RLocation(TestWorld.REF, 9.0, 72.0, 9.0, 0.0f, 0.0f));

        assertSame(entity, hurt.entity());
        assertSame(entity, spawn.entity());
        assertSame(entity, hurt.livingEntity().orElseThrow());
        assertSame(entity, spawn.livingEntity().orElseThrow());
        assertEquals(new RBlockPos(1, 70, 2), hurt.pos());
        assertEquals(new RBlockPos(1, 70, 2), spawn.pos());
        assertEquals(RKey.of("test:fire"), hurt.damageTypeKey());
        assertEquals("NATURAL", spawn.reason());
    }

    @Test
    void payloadsLeaveLivingAccessEmptyForNonLivingEntities() {
        MutableEntity entity = new MutableEntity("test:boat", new RLocation(TestWorld.REF, 2.0, 64.0, 2.0, 0.0f, 0.0f));

        AttackEntityPost attack = new AttackEntityPost(new TestServerPlayer(), entity, false);
        EntityHurtPre hurt = new EntityHurtPre(entity, "test:collision", false);

        assertTrue(attack.livingEntity().isEmpty());
        assertTrue(hurt.livingEntity().isEmpty());
    }

    @Test
    void helperPostsCaptureLiveEntityMetadataOnce() {
        MutableLivingEntity entity = new MutableLivingEntity("test:enderman", new RLocation(TestWorld.REF, 3.8, 66.0, 9.2, 0.0f, 0.0f));
        TestServerPlayer player = new TestServerPlayer();

        AttackEntityPost attack = EntityEventPayloads.attackPost(player, entity, true);
        InteractEntityPost interact = EntityEventPayloads.interactPost(player, entity, false);

        entity.moveTo(new RLocation(TestWorld.REF, 14.0, 79.0, -1.0, 0.0f, 0.0f));

        assertSame(entity, attack.entity());
        assertSame(entity, interact.entity());
        assertEquals(new RBlockPos(3, 66, 9), attack.pos());
        assertEquals(new RBlockPos(3, 66, 9), interact.pos());
        assertEquals(TestWorld.REF, attack.world());
        assertEquals(TestWorld.REF, interact.world());
        assertTrue(attack.cancelled());
    }

    @Test
    void helperSnapshotsCaptureLiveEntityMetadataOnce() {
        MutableLivingEntity entity = new MutableLivingEntity("test:creeper", new RLocation(TestWorld.REF, 5.9, 68.0, 3.1, 0.0f, 0.0f));

        EntitySpawnSnapshot spawn = EntityEventPayloads.spawnSnapshot(entity, "SPAWNER", true);
        EntityHurtSnapshot hurt = EntityEventPayloads.hurtSnapshot(entity, "test:explosion", false);

        entity.moveTo(new RLocation(TestWorld.REF, 20.0, 80.0, 20.0, 0.0f, 0.0f));

        assertEquals(TestWorld.REF, spawn.world());
        assertEquals(new RBlockPos(5, 68, 3), spawn.pos());
        assertEquals(RKey.of("test:creeper"), spawn.entityTypeKey());
        assertEquals("SPAWNER", spawn.reason());
        assertTrue(spawn.cancelled());

        assertEquals(TestWorld.REF, hurt.world());
        assertEquals(new RBlockPos(5, 68, 3), hurt.pos());
        assertEquals(RKey.of("test:creeper"), hurt.entityTypeKey());
        assertEquals(RKey.of("test:explosion"), hurt.damageTypeKey());
    }

    private static final class TestServerPlayer extends RNativeHandle<Object> implements RServerPlayer {
        private TestServerPlayer() {
            super(PlatformId.PAPER, new Object());
        }

        @Override
        public @NotNull Audience audience() {
            return Audience.empty();
        }

        @Override
        public @NotNull UUID uuid() {
            return UUID.fromString("00000000-0000-0000-0000-000000000111");
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
            return Optional.of(new TestWorld());
        }

        @Override
        public @NotNull Optional<RLocation> location() {
            return Optional.of(new RLocation(TestWorld.REF, 0.0, 64.0, 0.0, 0.0f, 0.0f));
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

    private static class MutableEntity extends RNativeHandle<Object> implements REntity {
        private final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000222");
        private final RKey typeKey;
        private RLocation location;

        private MutableEntity(String typeKey, RLocation location) {
            super(PlatformId.PAPER, new Object());
            this.typeKey = RKey.of(typeKey);
            this.location = location;
        }

        void moveTo(RLocation newLocation) {
            this.location = newLocation;
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
            return Optional.of(location);
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

    private static final class MutableLivingEntity extends MutableEntity implements RLivingEntity {
        private MutableLivingEntity(String typeKey, RLocation location) {
            super(typeKey, location);
        }

        @Override
        public double health() {
            return 10.0d;
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

    private static final class TestWorld extends RNativeHandle<Object> implements RWorld {
        private static final RWorldRef REF = new RWorldRef("world", "test:world");

        private TestWorld() {
            super(PlatformId.PAPER, new Object());
        }

        @Override
        public @NotNull RWorldRef ref() {
            return REF;
        }
    }
}
