package de.t14d3.rapunzellib.gui.paper;

import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PaperGuiFeatureInstallerSupportManifestTest {
    @Test
    void inventoryEventBridgeSupportIsReportedAsPartial() {
        assertEquals(
            GameEventSupportParity.PARTIAL,
            new PaperGuiFeatureInstaller().supportManifest().support(InventoryActionPre.class).parity()
        );
    }
}
