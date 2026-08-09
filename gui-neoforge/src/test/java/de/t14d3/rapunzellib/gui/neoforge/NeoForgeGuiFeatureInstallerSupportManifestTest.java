package de.t14d3.rapunzellib.gui.neoforge;

import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NeoForgeGuiFeatureInstallerSupportManifestTest {
    @Test
    void inventoryEventBridgeSupportRemainsEmulated() {
        assertEquals(
            GameEventSupportParity.EMULATED,
            new NeoForgeGuiFeatureInstaller().supportManifest().support(InventoryActionPre.class).parity()
        );
    }
}
