package de.t14d3.rapunzellib.events.fabric;

import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.interact.UseBlockPost;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
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
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntityHurtPost.class).parity());
        assertEquals(GameEventSupportParity.EMULATED, manifest.support(EntityHurtSnapshot.class).parity());
        assertEquals(GameEventSupportParity.PARTIAL, manifest.support(UseBlockPost.class).parity());
        assertEquals(GameEventSupportParity.PARTIAL, manifest.support(UseBlockSnapshot.class).parity());
        assertEquals(GameEventSupportParity.UNSUPPORTED, manifest.support(InventoryClickPre.class).parity());
        assertEquals(GameEventSupportParity.UNSUPPORTED, manifest.support(InventoryClickPost.class).parity());
        assertEquals(GameEventSupportParity.UNSUPPORTED, manifest.support(InventoryOpenPre.class).parity());
        assertEquals(GameEventSupportParity.UNSUPPORTED, manifest.support(InventoryOpenPost.class).parity());
        assertEquals(GameEventSupportParity.UNSUPPORTED, manifest.support(InventoryClosePost.class).parity());
    }
}
