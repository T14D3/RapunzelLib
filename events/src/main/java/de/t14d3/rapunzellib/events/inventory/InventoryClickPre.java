package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class InventoryClickPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RInventory inventory;
    private final int slot;
    private final InventoryClickType clickType;
    private final RItem currentItem;

    /**
     * Creates a new InventoryClickPre event.
     *
     * @param player    the clicking player
     * @param inventory the inventory being clicked
     * @param slot      the slot index
     * @param clickType the type of click
     */
    public InventoryClickPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        int slot,
        @NotNull InventoryClickType clickType
    ) {
        this(player, inventory, slot, clickType, false);
    }

    /**
     * Creates a new InventoryClickPre event with cancelled state.
     *
     * @param player    the clicking player
     * @param inventory the inventory being clicked
     * @param slot      the slot index
     * @param clickType the type of click
     * @param cancelled whether the event is initially cancelled
     */
    public InventoryClickPre(
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
        setCancelled(cancelled);
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
     * Returns the inventory being clicked.
     *
     * @return the inventory
     */
    public @NotNull RInventory inventory() {
        return inventory;
    }

    /**
     * Returns the slot index being clicked.
     *
     * @return the slot index
     */
    public int slot() {
        return slot;
    }

    /**
     * Returns the type of click performed.
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
