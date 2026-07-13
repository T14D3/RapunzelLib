package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Factory methods for creating inventory event payloads.
 *
 * <p>Provides convenience methods for constructing pre-event, post-event instances.</p>
 */
public final class InventoryEventPayloads {
    private InventoryEventPayloads() {
    }

    public static @NotNull InventoryOpenPre openPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory
    ) {
        return new InventoryOpenPre(player, inventory);
    }

    public static @NotNull InventoryOpenPost openPost(
        @NotNull RPlayer player,
        @NotNull RInventory inventory
    ) {
        return new InventoryOpenPost(player, inventory);
    }

    public static @NotNull InventoryClickPre clickPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        int slot,
        @NotNull InventoryClickType clickType
    ) {
        return new InventoryClickPre(player, inventory, slot, clickType);
    }

    public static @NotNull InventoryClickPost clickPost(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        int slot,
        @NotNull InventoryClickType clickType,
        boolean cancelled
    ) {
        return new InventoryClickPost(player, inventory, slot, clickType, cancelled);
    }

    public static @NotNull InventoryClosePost closePost(
        @NotNull RPlayer player,
        @NotNull RInventory inventory
    ) {
        return new InventoryClosePost(player, inventory);
    }
}
