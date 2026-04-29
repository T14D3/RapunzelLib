package de.t14d3.rapunzellib.gui.shared;

import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import org.jetbrains.annotations.NotNull;

public final class SharedGuiClickTypes {
    private SharedGuiClickTypes() {
    }

    public static @NotNull InventoryClickType mapMenuClick(
        // #if VERSION >= 26.0.0
        @NotNull net.minecraft.world.inventory.ContainerInput clickType,
        // #else
        // # @NotNull net.minecraft.world.inventory.ClickType clickType,
        // #endif
        int button
    ) {
        return switch (clickType) {
            case PICKUP -> button == 0 ? InventoryClickType.LEFT : InventoryClickType.RIGHT;
            case QUICK_MOVE -> button == 0 ? InventoryClickType.SHIFT_LEFT : InventoryClickType.SHIFT_RIGHT;
            case SWAP -> button == 40 ? InventoryClickType.SWAP_OFFHAND : InventoryClickType.NUMBER_KEY_1;
            case CLONE -> InventoryClickType.MIDDLE;
            case THROW -> button == 1 ? InventoryClickType.CONTROL_DROP : InventoryClickType.DROP;
            case QUICK_CRAFT -> InventoryClickType.UNKNOWN;
            case PICKUP_ALL -> InventoryClickType.DOUBLE_CLICK;
        };
    }
}
