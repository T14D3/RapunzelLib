package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    /**
     * Creates an {@link InventoryActionPre} for a single-slot click, capturing
     * the current item from the inventory slot.
     */
    public static @NotNull InventoryActionPre actionPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        int slot,
        @NotNull InventoryActionType actionType
    ) {
        RItem current = slot >= 0 && slot < inventory.size() ? inventory.item(slot).orElse(null) : null;
        return new InventoryActionPre(player, inventory, List.of(slot), actionType, null, current, false);
    }

    /**
     * Creates an {@link InventoryActionPre} for a multi-slot action (e.g. a drag).
     */
    public static @NotNull InventoryActionPre actionPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        @NotNull List<Integer> slots,
        @NotNull InventoryActionType actionType,
        @Nullable RItem cursorItem,
        @Nullable RItem currentItem
    ) {
        return new InventoryActionPre(player, inventory, slots, actionType, cursorItem, currentItem, false);
    }

    /**
     * Creates an {@link InventoryActionPost}.
     */
    public static @NotNull InventoryActionPost actionPost(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        @NotNull List<Integer> slots,
        @NotNull InventoryActionType actionType,
        @Nullable RItem cursorItem,
        @Nullable RItem currentItem,
        boolean cancelled
    ) {
        return new InventoryActionPost(player, inventory, slots, actionType, cursorItem, currentItem, null, cancelled);
    }

    public static @NotNull InventoryClosePost closePost(
        @NotNull RPlayer player,
        @NotNull RInventory inventory
    ) {
        return new InventoryClosePost(player, inventory);
    }
}
