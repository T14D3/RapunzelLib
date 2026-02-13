package de.t14d3.rapunzellib.gui.neoforge;

import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NeoForgeGuiFeatureInstallerSupportManifestTest {
    @Test
    void inventoryEventBridgeSupportRemainsEmulated() {
        assertEquals(
            GameEventSupportParity.EMULATED,
            new NeoForgeGuiFeatureInstaller().supportManifest().support(InventoryClickPre.class).parity()
        );
    }
}
