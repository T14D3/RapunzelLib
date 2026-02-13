package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlayerRolesTest {
    @Test
    void narrowsPlayersIntoExplicitRoleContracts() {
        TestServerPlayer serverPlayer = new TestServerPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000101"),
            new Object()
        );
        TestProxyPlayer proxyPlayer = new TestProxyPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000202"),
            new Object(),
            "lobby"
        );

        assertTrue(serverPlayer.isServerPlayer());
        assertFalse(serverPlayer.isProxyPlayer());
        assertTrue(serverPlayer.isLivingEntity());
        assertSame(serverPlayer, serverPlayer.asServerPlayer().orElseThrow());
        assertSame(serverPlayer, serverPlayer.asLivingEntity().orElseThrow());
        assertEquals(TestServerPlayer.WORLD.ref(), serverPlayer.worldOrThrow().ref());
        assertEquals(TestServerPlayer.LOCATION, serverPlayer.locationOrThrow());
        assertEquals(18.0d, serverPlayer.health());
        assertEquals(20.0d, serverPlayer.maxHealth());
        assertEquals(285, serverPlayer.remainingAir());
        assertEquals(300, serverPlayer.maxAir());
        assertFalse(serverPlayer.isDead());
        assertTrue(serverPlayer.canTeleport());

        assertTrue(serverPlayer.teleport(TestServerPlayer.TELEPORT_TARGET));

        assertEquals(TestServerPlayer.TELEPORT_TARGET, serverPlayer.lastTeleport);
        assertThrows(IllegalStateException.class, serverPlayer::requireProxyPlayer);

        assertTrue(proxyPlayer.isProxyPlayer());
        assertFalse(proxyPlayer.isServerPlayer());
        assertFalse(proxyPlayer.isLivingEntity());
        assertSame(proxyPlayer, proxyPlayer.asProxyPlayer().orElseThrow());
        assertEquals("lobby", proxyPlayer.currentServerNameOrThrow());
        assertThrows(IllegalStateException.class, proxyPlayer::requireServerPlayer);
        assertThrows(IllegalStateException.class, proxyPlayer::requireLivingEntity);
    }

    @Test
    void playersHelpersFilterAndRequireMatchingRoles() {
        TestServerPlayer serverPlayer = new TestServerPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000303"),
            "server-handle"
        );
        TestProxyPlayer proxyPlayer = new TestProxyPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000404"),
            "proxy-handle",
            "minigames-1"
        );
        TestPlayers players = new TestPlayers(serverPlayer, proxyPlayer);

        assertEquals(java.util.List.of(serverPlayer), players.onlineServers().stream().toList());
        assertEquals(java.util.List.of(proxyPlayer), players.onlineProxies().stream().toList());
        assertSame(serverPlayer, players.getServer(serverPlayer.uuid()).orElseThrow());
        assertSame(proxyPlayer, players.getProxy(proxyPlayer.uuid()).orElseThrow());
        assertSame(serverPlayer, players.wrapServer(serverPlayer.handle()).orElseThrow());
        assertSame(proxyPlayer, players.wrapProxy(proxyPlayer.handle()).orElseThrow());
        assertTrue(players.getServer(proxyPlayer.uuid()).isEmpty());
        assertTrue(players.wrapProxy(serverPlayer.handle()).isEmpty());
        assertSame(serverPlayer, players.requireServer(serverPlayer.uuid()));
        assertSame(proxyPlayer, players.requireProxy(proxyPlayer.handle()));
        assertThrows(IllegalArgumentException.class, () -> players.requireServer(proxyPlayer.uuid()));
        assertThrows(IllegalArgumentException.class, () -> players.requireProxy(serverPlayer.handle()));
    }

    private abstract static class TestPlayerBase extends RNativeHandle<Object> implements RPlayer {
        private final UUID uuid;
        private final String name;

        private TestPlayerBase(PlatformId platformId, UUID uuid, Object handle, String name) {
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

    private static final class TestServerPlayer extends TestPlayerBase implements RServerPlayer {
        private static final TestWorld WORLD = new TestWorld();
        private static final RLocation LOCATION = new RLocation(WORLD.ref(), 12.5, 64.0, -4.25, 90.0f, 15.0f);
        private static final RLocation TELEPORT_TARGET = new RLocation(WORLD.ref(), 1.0, 70.0, 2.0, 180.0f, 30.0f);

        private RLocation lastTeleport;

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
            return 18.0d;
        }

        @Override
        public double maxHealth() {
            return 20.0d;
        }

        @Override
        public int remainingAir() {
            return 285;
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
        public boolean canTeleport() {
            return true;
        }

        @Override
        public boolean teleport(@NotNull RLocation location) {
            lastTeleport = location;
            return true;
        }
    }

    private static final class TestProxyPlayer extends TestPlayerBase implements RProxyPlayer {
        private final String currentServerName;

        private TestProxyPlayer(UUID uuid, Object handle, String currentServerName) {
            super(PlatformId.VELOCITY, uuid, handle, "proxy-player");
            this.currentServerName = currentServerName;
        }

        @Override
        public @NotNull Optional<String> currentServerName() {
            return Optional.of(currentServerName);
        }
    }

    private static final class TestWorld extends RNativeHandle<Object> implements RWorld {
        private TestWorld() {
            super(PlatformId.PAPER, new Object());
        }

        @Override
        public @NotNull RWorldRef ref() {
            return new RWorldRef("spawn", "minecraft:overworld");
        }
    }

    private static final class TestPlayers implements Players {
        private final Map<UUID, RPlayer> playersById = new LinkedHashMap<>();
        private final Map<Object, RPlayer> playersByHandle = new LinkedHashMap<>();

        private TestPlayers(RPlayer... players) {
            for (RPlayer player : players) {
                playersById.put(player.uuid(), player);
                playersByHandle.put(player.handle(), player);
            }
        }

        @Override
        public @NotNull Collection<RPlayer> online() {
            return playersById.values();
        }

        @Override
        public @NotNull Optional<RPlayer> get(@NotNull UUID uuid) {
            return Optional.ofNullable(playersById.get(uuid));
        }

        @Override
        public @NotNull Optional<RPlayer> wrap(@NotNull Object nativePlayer) {
            return Optional.ofNullable(playersByHandle.get(nativePlayer));
        }
    }
}
