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

    public InventoryClickPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        int slot,
        @NotNull InventoryClickType clickType
    ) {
        this(player, inventory, slot, clickType, false);
    }

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

    public @NotNull RPlayer player() {
        return player;
    }

    public @NotNull RInventory inventory() {
        return inventory;
    }

    public int slot() {
        return slot;
    }

    public @NotNull InventoryClickType clickType() {
        return clickType;
    }

    public @NotNull Optional<RItem> currentItem() {
        return Optional.ofNullable(currentItem);
    }

    private static int requireSlot(int slot, int size) {
        if (slot < 0 || slot >= size) {
            throw new IndexOutOfBoundsException("Slot " + slot + " out of bounds for inventory size " + size);
        }
        return slot;
    }
}
