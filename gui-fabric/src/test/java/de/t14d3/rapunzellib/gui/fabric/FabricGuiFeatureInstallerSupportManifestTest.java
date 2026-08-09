package de.t14d3.rapunzellib.gui.fabric;

import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FabricGuiFeatureInstallerSupportManifestTest {
    @Test
    void inventoryEventBridgeSupportIsReportedAsPartial() {
        assertEquals(
            GameEventSupportParity.PARTIAL,
            new FabricGuiFeatureInstaller().supportManifest().support(InventoryActionPre.class).parity()
        );
    }
}
