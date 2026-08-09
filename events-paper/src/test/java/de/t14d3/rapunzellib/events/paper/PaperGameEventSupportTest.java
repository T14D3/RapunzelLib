package de.t14d3.rapunzellib.events.paper;

import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.block.BlockTransformPre;
import de.t14d3.rapunzellib.events.block.PistonMovePre;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.EntityDeathPost;
import de.t14d3.rapunzellib.events.entity.EntityDeathPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityTamePost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPost;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.player.InteractBlockPost;
import de.t14d3.rapunzellib.events.player.PlayerJoinPost;
import de.t14d3.rapunzellib.events.player.PlayerLoginPre;
import de.t14d3.rapunzellib.events.player.PlayerMessagePost;
import de.t14d3.rapunzellib.events.player.PlayerMessagePre;
import de.t14d3.rapunzellib.events.player.PlayerStatePost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PaperGameEventSupportTest {
    @Test
    void manifestKeepsRichEntityParityNative() {
        GameEventSupportManifest manifest = new PaperGameEventBridgeInstaller().supportManifest();

        assertEquals(GameEventSupportParity.NATIVE, manifest.support(AttackEntityPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InteractEntityPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntitySpawnPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntitySpawnSnapshot.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityHurtPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityHurtSnapshot.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryActionPre.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryActionPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryOpenPre.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryOpenPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryClosePost.class).parity());
    }

    @Test
    void manifestDeclaresCatalogWaveEventsNative() {
        GameEventSupportManifest manifest = new PaperGameEventBridgeInstaller().supportManifest();

        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InteractBlockPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityDeathPre.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityDeathPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityTamePost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(PlayerJoinPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(PlayerLoginPre.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(PlayerMessagePre.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(PlayerMessagePost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(PlayerStatePost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(BlockTransformPre.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(PistonMovePre.class).parity());
    }
}
