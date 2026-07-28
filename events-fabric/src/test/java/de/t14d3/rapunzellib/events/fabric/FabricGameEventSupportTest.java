package de.t14d3.rapunzellib.events.fabric;

import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.block.BlockDestroyPre;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPost;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPre;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityMovePost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityTeleportPost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.interact.UseBlockPost;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.player.PlayerMovePost;
import de.t14d3.rapunzellib.events.player.PlayerMovePre;
import de.t14d3.rapunzellib.events.world.ExplosionPre;
import de.t14d3.rapunzellib.events.world.TntPrimePre;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FabricGameEventSupportTest {
    @Test
    void manifestDocumentsCurrentEntityPostAndSnapshotCoverage() {
        GameEventSupportManifest manifest = new FabricGameEventBridgeInstaller().supportManifest();

        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntitySpawnPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntitySpawnSnapshot.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(AttackEntityPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InteractEntityPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntityHurtPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntityHurtPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntityHurtSnapshot.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntityTeleportPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntityMovePost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(PlayerMovePre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(PlayerMovePost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(ExplosionPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(TntPrimePre.class).parity());
        assertEquals(GameEventSupportParity.PARTIAL, manifest.support(UseBlockPost.class).parity());
        assertEquals(GameEventSupportParity.PARTIAL, manifest.support(UseBlockSnapshot.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryClickPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryClickPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryOpenPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryOpenPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryClosePost.class).parity());
    }

    @Test
    void blockMixinEventsAreEmulated() {
        GameEventSupportManifest manifest = new FabricGameEventBridgeInstaller().supportManifest();

        assertEquals(GameEventSupportParity.EMULATED, manifest.support(BlockPhysicsPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(BlockPhysicsPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(BlockDestroyPre.class).parity());
    }
}