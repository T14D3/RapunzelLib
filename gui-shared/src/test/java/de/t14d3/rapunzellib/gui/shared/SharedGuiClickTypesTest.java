package de.t14d3.rapunzellib.gui.shared;

import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SharedGuiClickTypesTest {
    @Test
    void swapOffhandMapsToDedicatedClickType() {
        assertEquals(
            InventoryClickType.SWAP_OFFHAND,
            SharedGuiClickTypes.mapMenuClick(net.minecraft.world.inventory.ClickType.SWAP, 40)
        );
    }

    @Test
    void controlDropMapsToDedicatedClickType() {
        assertEquals(
            InventoryClickType.CONTROL_DROP,
            SharedGuiClickTypes.mapMenuClick(net.minecraft.world.inventory.ClickType.THROW, 1)
        );
    }
}
