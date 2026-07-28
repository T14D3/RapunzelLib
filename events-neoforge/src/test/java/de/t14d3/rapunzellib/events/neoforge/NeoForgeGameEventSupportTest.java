package de.t14d3.rapunzellib.events.neoforge;

import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.block.BlockDestroyPre;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPost;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPre;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityMovePost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPre;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.player.PlayerMovePost;
import de.t14d3.rapunzellib.events.player.PlayerMovePre;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NeoForgeGameEventSupportTest {
    @Test
    void manifestTracksExpandedEntityParity() {
        GameEventSupportManifest manifest = new NeoForgeGameEventBridgeInstaller().supportManifest();

        assertEquals(GameEventSupportParity.NATIVE, manifest.support(AttackEntityPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InteractEntityPost.class).parity());
        assertEquals(GameEventSupportParity.PARTIAL, manifest.support(EntitySpawnPre.class).parity());
        assertEquals(GameEventSupportParity.PARTIAL, manifest.support(EntitySpawnPost.class).parity());
        assertEquals(GameEventSupportParity.PARTIAL, manifest.support(EntitySpawnSnapshot.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityHurtPre.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityHurtPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(EntityHurtSnapshot.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryClickPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryClickPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryOpenPre.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryOpenPost.class).parity());
        assertEquals(GameEventSupportParity.NATIVE, manifest.support(InventoryClosePost.class).parity());
    }

    @Test
    void emulatedMixinsPresent() {
        GameEventSupportManifest manifest = new NeoForgeGameEventBridgeInstaller().supportManifest();

        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntityMovePost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(PlayerMovePre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(PlayerMovePost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(BlockPhysicsPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(BlockPhysicsPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(BlockDestroyPre.class).parity());
    }
}