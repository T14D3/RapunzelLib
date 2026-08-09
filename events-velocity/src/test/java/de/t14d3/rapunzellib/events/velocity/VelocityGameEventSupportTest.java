package de.t14d3.rapunzellib.events.velocity;

import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.entity.EntityHurtPre;
import de.t14d3.rapunzellib.events.player.PlayerJoinPost;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VelocityGameEventSupportTest {
    @Test
    void manifestShowsProxyJoinQuitCoverage() {
        GameEventSupportManifest manifest = new VelocityGameEventBridgeInstaller().supportManifest();

        assertEquals(GameEventSupportParity.NATIVE, manifest.support(PlayerJoinPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(PlayerQuitPost.class).parity());
        // Game-server events have no proxy equivalent.
        assertEquals(GameEventSupportParity.UNSUPPORTED, manifest.support(EntityHurtPre.class).parity());
    }
}
