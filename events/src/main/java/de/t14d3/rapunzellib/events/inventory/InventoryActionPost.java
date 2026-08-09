package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Post-event fired after a player performed an inventory action (click or
 * drag).
 *
 * <p>Carries the same payload as {@link InventoryActionPre} plus the final
 * cancelled state. See {@link InventoryActionPre} for the raw-slot semantics
 * of {@link #slots()}.</p>
 *
 * @param player       the acting player
 * @param inventory    the inventory the action targeted
 * @param slots        the raw slots involved (single-element for clicks, full list for drags)
 * @param actionType   the action type
 *  @param cursorItem   the item on the cursor, or {@code null} when empty or unknown
 *  @param currentItem  the item in the first affected slot, or {@code null} when empty or out of bounds
 *  @param hotbarButton the hotbar slot (0-8) for NUMBER_KEY clicks, or {@code null} otherwise
 *  @param cancelled    whether the action was cancelled
 */
public record InventoryActionPost(
    @NotNull RPlayer player,
    @NotNull RInventory inventory,
    @NotNull List<Integer> slots,
    @NotNull InventoryActionType actionType,
    @Nullable RItem cursorItem,
    @Nullable RItem currentItem,
    @Nullable Integer hotbarButton,
    boolean cancelled
) implements GamePostEvent {

    public InventoryActionPost {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(slots, "slots");
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("slots must not be empty");
        }
        slots = List.copyOf(slots);
        Objects.requireNonNull(actionType, "actionType");
    }

    /**
     * Whether this action is a drag (convenience for
     * {@code actionType() == InventoryActionType.DRAG}).
     *
     * @return true for drag actions
     */
    public boolean isDrag() {
        return actionType == InventoryActionType.DRAG;
    }

    /**
     * Returns the item on the cursor, if any.
     *
     * @return the cursor item, or empty when the cursor is empty or unknown
     */
    public @NotNull Optional<RItem> cursorItemIfPresent() {
        return Optional.ofNullable(cursorItem);
    }

    /**
     * Returns the item currently in the first affected slot, if any.
     *
     * @return the current item, or empty when the slot is empty or out of bounds
     */
    public @NotNull Optional<RItem> currentItemIfPresent() {
        return Optional.ofNullable(currentItem);
    }

    /**
     * Returns the hotbar slot (0-8) involved in this action, when it is a
     * {@link InventoryActionType#NUMBER_KEY} click.
     *
     * <p>Mirrors Bukkit's {@code InventoryClickEvent#getHotbarButton()}: the
     * slot whose item is swapped with the clicked slot. Empty for every other
     * action type and on platforms that do not expose the button.</p>
     *
     * @return the hotbar button slot, or empty when not a NUMBER_KEY action
     */
    public @NotNull Optional<Integer> hotbarButtonIfPresent() {
        return Optional.ofNullable(hotbarButton);
    }
}
