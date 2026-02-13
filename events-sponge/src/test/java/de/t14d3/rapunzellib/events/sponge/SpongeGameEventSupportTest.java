package de.t14d3.rapunzellib.events.sponge;

import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
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
        assertEquals(GameEventSupportParity.UNSUPPORTED, manifest.support(InventoryClickPre.class).parity());
        assertEquals(GameEventSupportParity.UNSUPPORTED, manifest.support(InventoryClickPost.class).parity());
        assertEquals(GameEventSupportParity.UNSUPPORTED, manifest.support(InventoryOpenPre.class).parity());
        assertEquals(GameEventSupportParity.UNSUPPORTED, manifest.support(InventoryOpenPost.class).parity());
        assertEquals(GameEventSupportParity.UNSUPPORTED, manifest.support(InventoryClosePost.class).parity());
    }
}
