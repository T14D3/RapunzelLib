package de.t14d3.rapunzellib.gui.inventory;

import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import de.t14d3.rapunzellib.gui.context.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GuiInventoryClickTypes {
    private GuiInventoryClickTypes() {
    }

    public static @NotNull ClickType fromEventClickType(@NotNull InventoryClickType clickType) {
        Objects.requireNonNull(clickType, "clickType");
        return switch (clickType) {
            case LEFT -> ClickType.LEFT;
            case RIGHT -> ClickType.RIGHT;
            case SHIFT_LEFT -> ClickType.SHIFT_LEFT;
            case SHIFT_RIGHT -> ClickType.SHIFT_RIGHT;
            case MIDDLE -> ClickType.MIDDLE;
            case DOUBLE_CLICK -> ClickType.DOUBLE_CLICK;
            case DROP -> ClickType.DROP;
            case CONTROL_DROP -> ClickType.CONTROL_DROP;
            case NUMBER_KEY_1 -> ClickType.NUMBER_KEY_1;
            case SWAP_OFFHAND -> ClickType.SWAP_OFFHAND;
            case UNKNOWN -> ClickType.UNKNOWN;
        };
    }
}
