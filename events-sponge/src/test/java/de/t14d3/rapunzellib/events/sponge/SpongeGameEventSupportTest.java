package de.t14d3.rapunzellib.events.sponge;

import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityMovePost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityTeleportPost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPost;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.events.player.PlayerMovePost;
import de.t14d3.rapunzellib.events.player.PlayerMovePre;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SpongeGameEventSupportTest {
    @Test
    void manifestShowsNativeRichEntityCoverage() {
        GameEventSupportManifest manifest = new SpongeGameEventBridgeInstaller().supportManifest();

        assertEquals(GameEventSupportParity.NATIVE, manifest.support(AttackEntityPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InteractEntityPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntitySpawnPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntitySpawnSnapshot.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityHurtPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityHurtSnapshot.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityTeleportPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityMovePost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(PlayerMovePre.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(PlayerMovePost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryActionPre.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryActionPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryOpenPre.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryOpenPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryClosePost.class).parity());
        assertEquals(GameEventSupportParity.PARTIAL, manifest.support(InteractBlockPre.class).parity());
    }
}