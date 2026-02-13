package de.t14d3.rapunzellib.gui.sponge;

import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SpongeGuiFeatureInstallerSupportManifestTest {
    @Test
    void inventoryEventBridgeSupportIsReportedAsPartial() {
        assertEquals(
            GameEventSupportParity.PARTIAL,
            new SpongeGuiFeatureInstaller().supportManifest().support(InventoryClickPre.class).parity()
        );
    }
}
