package de.t14d3.rapunzellib.gui.sponge.inventory;

import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.item.inventory.menu.ClickTypes;

public final class SpongeGuiClickTypes {
    private SpongeGuiClickTypes() {
    }

    public static @NotNull InventoryClickType mapInventoryClick(@NotNull org.spongepowered.api.item.inventory.menu.ClickType<?> spongeClickType) {
        if (spongeClickType.equals(ClickTypes.CLICK_LEFT.get())) {
            return InventoryClickType.LEFT;
        }
        if (spongeClickType.equals(ClickTypes.CLICK_RIGHT.get())) {
            return InventoryClickType.RIGHT;
        }
        if (spongeClickType.equals(ClickTypes.CLICK_MIDDLE.get())) {
            return InventoryClickType.MIDDLE;
        }
        if (spongeClickType.equals(ClickTypes.SHIFT_CLICK_LEFT.get())) {
            return InventoryClickType.SHIFT_LEFT;
        }
        if (spongeClickType.equals(ClickTypes.SHIFT_CLICK_RIGHT.get())) {
            return InventoryClickType.SHIFT_RIGHT;
        }
        if (spongeClickType.equals(ClickTypes.DOUBLE_CLICK.get())) {
            return InventoryClickType.DOUBLE_CLICK;
        }
        return InventoryClickType.UNKNOWN;
    }
}
