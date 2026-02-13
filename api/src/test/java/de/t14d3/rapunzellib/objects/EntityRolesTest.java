package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EntityRolesTest {
    @Test
    void serverPlayersExposeEntitySemanticsWithoutAffectingProxyPlayers() {
        TestServerPlayer serverPlayer = new TestServerPlayer(UUID.fromString("00000000-0000-0000-0000-000000000505"), "server");
        TestEntity entity = new TestEntity(UUID.fromString("00000000-0000-0000-0000-000000000606"), "paper:zombie", "entity");
        TestLivingEntity livingEntity = new TestLivingEntity(UUID.fromString("00000000-0000-0000-0000-000000000616"), "paper:sheep", "living-entity");
        TestProxyPlayer proxyPlayer = new TestProxyPlayer(UUID.fromString("00000000-0000-0000-0000-000000000707"), "proxy");

        assertTrue(serverPlayer.isEntity());
        assertTrue(serverPlayer.isLivingEntity());
        assertSame(serverPlayer, serverPlayer.asEntity().orElseThrow());
        assertSame(serverPlayer, serverPlayer.asLivingEntity().orElseThrow());
        assertEquals(RKey.of("minecraft:player"), serverPlayer.typeKey());
        assertEquals(TestServerPlayer.LOCATION.blockPos(), serverPlayer.blockPos().orElseThrow());
        assertEquals(20.0d, serverPlayer.health());
        assertFalse(serverPlayer.isDead());

        assertTrue(entity.isPlayer() == false);
        assertFalse(entity.isLivingEntity());
        assertEquals(TestEntity.LOCATION.world(), entity.worldRef().orElseThrow());
        assertEquals(TestEntity.LOCATION.blockPos(), entity.blockPos().orElseThrow());

        assertTrue(livingEntity.isLivingEntity());
        assertSame(livingEntity, livingEntity.asLivingEntity().orElseThrow());
        assertEquals(8.0d, livingEntity.health());
        assertEquals(16.0d, livingEntity.maxHealth());
        assertEquals(250, livingEntity.remainingAir());
        assertEquals(300, livingEntity.maxAir());
        assertFalse(livingEntity.isDead());

        assertFalse(proxyPlayer.isEntity());
        assertFalse(proxyPlayer.isLivingEntity());
        assertTrue(proxyPlayer.asEntity().isEmpty());
        assertTrue(proxyPlayer.asLivingEntity().isEmpty());
    }

    @Test
    void entitiesServiceNarrowsPlayersAndGenericEntities() {
        TestServerPlayer serverPlayer = new TestServerPlayer(UUID.fromString("00000000-0000-0000-0000-000000000808"), "server");
        TestEntity entity = new TestEntity(UUID.fromString("00000000-0000-0000-0000-000000000909"), "paper:cow", "entity");
        TestLivingEntity livingEntity = new TestLivingEntity(UUID.fromString("00000000-0000-0000-0000-000000000919"), "paper:villager", "living-entity");
        TestEntities entities = new TestEntities(serverPlayer, entity, livingEntity);

        assertSame(serverPlayer, entities.getServerPlayer(serverPlayer.uuid()).orElseThrow());
        assertSame(serverPlayer, entities.getLivingEntity(serverPlayer.uuid()).orElseThrow());
        assertSame(serverPlayer, entities.wrapServerPlayer(serverPlayer.handle()).orElseThrow());
        assertSame(serverPlayer, entities.wrapLivingEntity(serverPlayer.handle()).orElseThrow());
        assertSame(serverPlayer, entities.requirePlayer(serverPlayer.uuid()));
        assertSame(livingEntity, entities.requireLivingEntity(livingEntity.uuid()));
        assertSame(livingEntity, entities.wrapLivingEntity(livingEntity.handle()).orElseThrow());
        assertSame(entity, entities.require(entity.handle()));
        assertSame(entity, entities.wrap(entity.handle()).orElseThrow());
        assertTrue(entities.getLivingEntity(entity.uuid()).isEmpty());
        assertTrue(entities.wrapPlayer(entity.handle()).isEmpty());
    }

    private abstract static class TestAudiencePlayer extends RNativeHandle<Object> implements RPlayer {
        private final UUID uuid;
        private final String name;

        private TestAudiencePlayer(PlatformId platformId, UUID uuid, Object handle, String name) {
            super(platformId, handle);
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public @NotNull Audience audience() {
            return Audience.empty();
        }

        @Override
        public @NotNull UUID uuid() {
            return uuid;
        }

        @Override
        public @NotNull String name() {
            return name;
        }

        @Override
        public boolean hasPermission(@NotNull String permission) {
            return true;
        }
    }

    private static final class TestServerPlayer extends TestAudiencePlayer implements RServerPlayer {
        private static final TestWorld WORLD = new TestWorld();
        private static final RLocation LOCATION = new RLocation(WORLD.ref(), 10.0, 65.0, -2.0, 45.0f, 15.0f);

        private TestServerPlayer(UUID uuid, Object handle) {
            super(PlatformId.PAPER, uuid, handle, "server-player");
        }

        @Override
        public @NotNull Optional<RWorld> world() {
            return Optional.of(WORLD);
        }

        @Override
        public @NotNull Optional<RLocation> location() {
            return Optional.of(LOCATION);
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

    private static final class TestProxyPlayer extends TestAudiencePlayer implements RProxyPlayer {
        private TestProxyPlayer(UUID uuid, Object handle) {
            super(PlatformId.VELOCITY, uuid, handle, "proxy-player");
        }

        @Override
        public @NotNull Optional<String> currentServerName() {
            return Optional.of("hub");
        }
    }

    private static final class TestEntity extends RNativeHandle<Object> implements REntity {
        private static final TestWorld WORLD = new TestWorld();
        private static final RLocation LOCATION = new RLocation(WORLD.ref(), 3.25, 70.0, 8.75, 0.0f, 0.0f);

        private final UUID uuid;
        private final RKey typeKey;

        private TestEntity(UUID uuid, String typeKey, Object handle) {
            super(PlatformId.PAPER, handle);
            this.uuid = uuid;
            this.typeKey = RKey.of(typeKey);
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
            return Optional.of(WORLD);
        }

        @Override
        public @NotNull Optional<RLocation> location() {
            return Optional.of(LOCATION);
        }
    }

    private static final class TestLivingEntity extends RNativeHandle<Object> implements RLivingEntity {
        private static final TestWorld WORLD = new TestWorld();
        private static final RLocation LOCATION = new RLocation(WORLD.ref(), 6.0, 64.0, 6.0, 0.0f, 0.0f);

        private final UUID uuid;
        private final RKey typeKey;

        private TestLivingEntity(UUID uuid, String typeKey, Object handle) {
            super(PlatformId.PAPER, handle);
            this.uuid = uuid;
            this.typeKey = RKey.of(typeKey);
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
            return Optional.of(WORLD);
        }

        @Override
        public @NotNull Optional<RLocation> location() {
            return Optional.of(LOCATION);
        }

        @Override
        public double health() {
            return 8.0d;
        }

        @Override
        public double maxHealth() {
            return 16.0d;
        }

        @Override
        public int remainingAir() {
            return 250;
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
        private TestWorld() {
            super(PlatformId.PAPER, new Object());
        }

        @Override
        public @NotNull RWorldRef ref() {
            return new RWorldRef("world", "minecraft:test");
        }
    }

    private static final class TestEntities implements Entities {
        private final Map<UUID, REntity> entitiesById = new LinkedHashMap<>();
        private final Map<Object, REntity> entitiesByHandle = new LinkedHashMap<>();

        private TestEntities(REntity... entities) {
            for (REntity entity : entities) {
                entitiesById.put(entity.uuid(), entity);
                entitiesByHandle.put(entity.handle(), entity);
            }
        }

        @Override
        public @NotNull Optional<REntity> get(@NotNull UUID uuid) {
            return Optional.ofNullable(entitiesById.get(uuid));
        }

        @Override
        public @NotNull Optional<REntity> wrap(@NotNull Object nativeEntity) {
            return Optional.ofNullable(entitiesByHandle.get(nativeEntity));
        }
    }
}
