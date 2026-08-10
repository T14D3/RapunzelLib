package de.t14d3.rapunzellib.events.fabric;

import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.block.BlockDestroyPre;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPost;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPre;
import de.t14d3.rapunzellib.events.block.PistonMovePre;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.EntityDeathPost;
import de.t14d3.rapunzellib.events.entity.EntityDeathPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityMovePost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityTamePost;
import de.t14d3.rapunzellib.events.entity.EntityTeleportPost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPost;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryTransferPre;
import de.t14d3.rapunzellib.events.player.InteractBlockPost;
import de.t14d3.rapunzellib.events.player.PlayerJoinPost;
import de.t14d3.rapunzellib.events.player.PlayerLoginPre;
import de.t14d3.rapunzellib.events.player.PlayerMessagePost;
import de.t14d3.rapunzellib.events.player.PlayerMessagePre;
import de.t14d3.rapunzellib.events.player.PlayerMovePost;
import de.t14d3.rapunzellib.events.player.PlayerMovePre;
import de.t14d3.rapunzellib.events.player.PlayerStatePost;
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
        assertEquals(GameEventSupportParity.PARTIAL, manifest.support(InteractBlockPost.class).parity());
        assertEquals(GameEventSupportParity.PARTIAL, manifest.support(UseBlockSnapshot.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryActionPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryActionPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryOpenPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryOpenPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryClosePost.class).parity());
    }

    @Test
    void manifestDocumentsSharedMixinDeathPistonTameLoginMessageAndStateCoverage() {
        GameEventSupportManifest manifest = new FabricGameEventBridgeInstaller().supportManifest();

        assertEquals(GameEventSupportParity.EMULATED, manifest.support(PistonMovePre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntityDeathPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntityDeathPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntityTamePost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(PlayerLoginPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(PlayerJoinPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(PlayerMessagePre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(PlayerMessagePost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(PlayerStatePost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(InventoryTransferPre.class).parity());
    }

    @Test
    void blockMixinEventsAreEmulated() {
        GameEventSupportManifest manifest = new FabricGameEventBridgeInstaller().supportManifest();

        assertEquals(GameEventSupportParity.EMULATED, manifest.support(BlockPhysicsPre.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(BlockPhysicsPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(BlockDestroyPre.class).parity());
    }
}