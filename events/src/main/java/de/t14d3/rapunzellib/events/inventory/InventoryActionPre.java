package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pre-event fired before a player performs an inventory action (click or
 * drag).
 *
 * <p>This event is cancellable. If denied, the action will not occur.</p>
 *
 * <p>{@link #slots()} carries the raw slots involved: a single-element list
 * for clicks, the full list of affected raw slots for drags. Raw slots may
 * index beyond {@link RInventory#size()} (the player inventory section of a
 * container view), so the list is not bounds-checked.</p>
 *
 * <p>{@link #hotbarButton()} carries the hotbar slot (0-8) for
 * {@link InventoryActionType#NUMBER_KEY} clicks - the slot whose item is
 * swapped with the clicked slot - and is empty for all other actions.</p>
 */
public final class InventoryActionPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RInventory inventory;
    private final List<Integer> slots;
    private final InventoryActionType actionType;
    private final RItem cursorItem;
    private final RItem currentItem;
    private final Integer hotbarButton;

    public InventoryActionPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        @NotNull List<Integer> slots,
        @NotNull InventoryActionType actionType
    ) {
        this(player, inventory, slots, actionType, null, null, null, false);
    }

    public InventoryActionPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        @NotNull List<Integer> slots,
        @NotNull InventoryActionType actionType,
        boolean cancelled
    ) {
        this(player, inventory, slots, actionType, null, null, null, cancelled);
    }

    public InventoryActionPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        @NotNull List<Integer> slots,
        @NotNull InventoryActionType actionType,
        @Nullable RItem cursorItem,
        @Nullable RItem currentItem
    ) {
        this(player, inventory, slots, actionType, cursorItem, currentItem, null, false);
    }

    public InventoryActionPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        @NotNull List<Integer> slots,
        @NotNull InventoryActionType actionType,
        @Nullable RItem cursorItem,
        @Nullable RItem currentItem,
        boolean cancelled
    ) {
        this(player, inventory, slots, actionType, cursorItem, currentItem, null, cancelled);
    }

    public InventoryActionPre(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        @NotNull List<Integer> slots,
        @NotNull InventoryActionType actionType,
        @Nullable RItem cursorItem,
        @Nullable RItem currentItem,
        @Nullable Integer hotbarButton,
        boolean cancelled
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.slots = List.copyOf(Objects.requireNonNull(slots, "slots"));
        if (this.slots.isEmpty()) {
            throw new IllegalArgumentException("slots must not be empty");
        }
        this.actionType = Objects.requireNonNull(actionType, "actionType");
        this.cursorItem = cursorItem;
        this.currentItem = currentItem;
        this.hotbarButton = hotbarButton;
        setCancelled(cancelled);
    }

    public @NotNull RPlayer player() {
        return player;
    }

    public @NotNull RInventory inventory() {
        return inventory;
    }

    /**
     * Returns the raw slots involved in this action: a single-element list
     * for clicks, the full list of affected raw slots for drags.
     *
     * @return the raw slots
     */
    public @NotNull List<Integer> slots() {
        return slots;
    }

    public @NotNull InventoryActionType actionType() {
        return actionType;
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
    public @NotNull Optional<RItem> cursorItem() {
        return Optional.ofNullable(cursorItem);
    }

    /**
     * Returns the item currently in the first affected slot, if any.
     *
     * @return the current item, or empty when the slot is empty or out of bounds
     */
    public @NotNull Optional<RItem> currentItem() {
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
    public @NotNull Optional<Integer> hotbarButton() {
        return Optional.ofNullable(hotbarButton);
    }
}
