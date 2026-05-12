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

    /**
     * Creates an {@link InventoryOpenPre} event.
     *
     * @param player    the player opening the inventory
     * @param inventory the inventory being opened
     * @return the pre-event
     */
    public static @NotNull InventoryOpenPre openPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory
    ) {
        return new InventoryOpenPre(player, inventory);
    }

    /**
     * Creates an {@link InventoryOpenPost} event.
     *
     * @param player    the player who opened the inventory
     * @param inventory the inventory that was opened
     * @return the post-event
     */
    public static @NotNull InventoryOpenPost openPost(
        @NotNull RPlayer player,
        @NotNull RInventory inventory
    ) {
        return new InventoryOpenPost(player, inventory);
    }

    /**
     * Creates an {@link InventoryClickPre} event.
     *
     * @param player    the clicking player
     * @param inventory the inventory being clicked
     * @param slot      the slot index
     * @param clickType the type of click
     * @return the pre-event
     */
    public static @NotNull InventoryClickPre clickPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        int slot,
        @NotNull InventoryClickType clickType
    ) {
        return new InventoryClickPre(player, inventory, slot, clickType);
    }

    /**
     * Creates an {@link InventoryClickPost} event.
     *
     * @param player    the clicking player
     * @param inventory the inventory that was clicked
     * @param slot      the slot index
     * @param clickType the type of click
     * @param cancelled whether the click was cancelled
     * @return the post-event
     */
    public static @NotNull InventoryClickPost clickPost(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        int slot,
        @NotNull InventoryClickType clickType,
        boolean cancelled
    ) {
        return new InventoryClickPost(player, inventory, slot, clickType, cancelled);
    }

    /**
     * Creates an {@link InventoryClosePost} event.
     *
     * @param player    the player who closed the inventory
     * @param inventory the inventory that was closed
     * @return the post-event
     */
    public static @NotNull InventoryClosePost closePost(
        @NotNull RPlayer player,
        @NotNull RInventory inventory
    ) {
        return new InventoryClosePost(player, inventory);
    }
}
