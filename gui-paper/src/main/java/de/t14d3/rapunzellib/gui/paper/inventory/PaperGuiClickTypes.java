package de.t14d3.rapunzellib.gui.paper.inventory;

import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import org.jetbrains.annotations.NotNull;

public final class PaperGuiClickTypes {
    private PaperGuiClickTypes() {
    }

    public static @NotNull InventoryClickType mapInventoryClick(@NotNull org.bukkit.event.inventory.ClickType bukkitClick) {
        return switch (bukkitClick) {
            case LEFT -> InventoryClickType.LEFT;
            case RIGHT -> InventoryClickType.RIGHT;
            case SHIFT_LEFT -> InventoryClickType.SHIFT_LEFT;
            case SHIFT_RIGHT -> InventoryClickType.SHIFT_RIGHT;
            case MIDDLE -> InventoryClickType.MIDDLE;
            case DOUBLE_CLICK -> InventoryClickType.DOUBLE_CLICK;
            case DROP -> InventoryClickType.DROP;
            case CONTROL_DROP -> InventoryClickType.CONTROL_DROP;
            case NUMBER_KEY -> InventoryClickType.NUMBER_KEY_1;
            case SWAP_OFFHAND -> InventoryClickType.SWAP_OFFHAND;
            default -> InventoryClickType.UNKNOWN;
        };
    }
}
