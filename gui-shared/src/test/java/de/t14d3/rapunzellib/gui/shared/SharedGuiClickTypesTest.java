package de.t14d3.rapunzellib.gui.shared;

import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SharedGuiClickTypesTest {
    @Test
    void swapOffhandMapsToDedicatedClickType() {
        assertEquals(
            InventoryClickType.SWAP_OFFHAND,
            // #if VERSION >= 26.0.0
            SharedGuiClickTypes.mapMenuClick(net.minecraft.world.inventory.ContainerInput.SWAP,
            // #else
            // # SharedGuiClickTypes.mapMenuClick(net.minecraft.world.inventory.ClickType.SWAP,
            // #endif
            40)
        );
    }

    @Test
    void controlDropMapsToDedicatedClickType() {
        assertEquals(
            InventoryClickType.CONTROL_DROP,
            // #if VERSION >= 26.0.0
            SharedGuiClickTypes.mapMenuClick(net.minecraft.world.inventory.ContainerInput.THROW,
            // #else
            // # SharedGuiClickTypes.mapMenuClick(net.minecraft.world.inventory.ClickType.THROW,
            // #endif
            1)
        );
    }
}
