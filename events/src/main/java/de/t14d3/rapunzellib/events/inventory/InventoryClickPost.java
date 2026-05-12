package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Post-event fired after a player has clicked in an inventory.
 *
 * <p>Contains the player, inventory, slot, click type, current item, and
 * whether the click was cancelled.</p>
 */
public final class InventoryClickPost implements GamePostEvent {
    private final RPlayer player;
    private final RInventory inventory;
    private final int slot;
    private final InventoryClickType clickType;
    private final RItem currentItem;
    private final boolean cancelled;

    /**
     * Creates a new InventoryClickPost event.
     *
     * @param player    the clicking player
     * @param inventory the inventory that was clicked
     * @param slot      the slot index
     * @param clickType the type of click
     * @param cancelled whether the click was cancelled
     */
    public InventoryClickPost(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        int slot,
        @NotNull InventoryClickType clickType,
        boolean cancelled
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.slot = requireSlot(slot, inventory.size());
        this.clickType = Objects.requireNonNull(clickType, "clickType");
        this.currentItem = inventory.item(slot).orElse(null);
        this.cancelled = cancelled;
    }

    /**
     * Returns the clicking player.
     *
     * @return the player
     */
    public @NotNull RPlayer player() {
        return player;
    }

    /**
     * Returns the inventory that was clicked.
     *
     * @return the inventory
     */
    public @NotNull RInventory inventory() {
        return inventory;
    }

    /**
     * Returns the slot that was clicked.
     *
     * @return the slot index
     */
    public int slot() {
        return slot;
    }

    /**
     * Returns the type of click that was performed.
     *
     * @return the click type
     */
    public @NotNull InventoryClickType clickType() {
        return clickType;
    }

    /**
     * Returns the current item in the clicked slot, if present.
     *
     * @return an optional containing the current item
     */
    public @NotNull Optional<RItem> currentItem() {
        return Optional.ofNullable(currentItem);
    }

    /**
     * Returns whether the click was cancelled.
     *
     * @return true if cancelled
     */
    public boolean cancelled() {
        return cancelled;
    }

    /**
     * Validates that the slot is within bounds for the inventory size.
     *
     * @param slot the slot index
     * @param size the inventory size
     * @return the validated slot index
     * @throws IndexOutOfBoundsException if the slot is out of bounds
     */
    private static int requireSlot(int slot, int size) {
        if (slot < 0 || slot >= size) {
            throw new IndexOutOfBoundsException("Slot " + slot + " out of bounds for inventory size " + size);
        }
        return slot;
    }
}
