package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable snapshot of a single container (player inventory or open screen)
 * as observed by a bot.
 *
 * <p>Slots are stored in the same linear order the Minecraft protocol uses
 * (top-left of the container first, then the player's main inventory, then
 * the hotbar). Slot indices match those used by the protocol for
 * {@code ServerboundContainerClickPacket} so a test author can pass the
 * slot straight back into {@link Bot#clickSlot(int, int, int, ClickType)}.</p>
 *
 * <p>{@code containerId} follows the protocol convention: {@code 0} for the
 * player's own inventory, and a positive number for whatever open container
 * the bot currently has (a chest, furnace, beacon, etc.).</p>
 */
public final class BotInventory {

    /** Click type parameter for {@link Bot#clickSlot(int, int, int, ClickType)}. */
    public enum ClickType {
        /** A plain left click in a slot: pick up or swap the stack. */
        LEFT_CLICK,
        /** A plain right click in a slot: pick up half / drop one item. */
        RIGHT_CLICK,
        /** Shift-click a slot, moving the stack according to container rules. */
        SHIFT_CLICK,
        /** Number-key 1..9 swap; the {@code button} parameter becomes 0..8. */
        HOTBAR_SWAP,
        /** Throw a single item out of the stack (drop-one), used with slot -999 for "throw while holding". */
        DROP_ONE,
        /** Throw the entire stack out (drop-stack). */
        DROP_ALL,
        /** "Pick block" creative grab action. */
        CREATIVE_GRAB
    }

    private final int containerId;
    private final int stateId;
    private final @NotNull BotItemStack @NotNull [] slots;
    private final @NotNull BotItemStack cursorItem;

    /**
     * Constructs a snapshot.
     *
     * @param containerId the protocol container id (0 for player inventory)
     * @param stateId     the state id echoed by the last synchronisation packet
     * @param slots       the slot contents, indexed by protocol slot number
     * @param cursorItem  the stack currently held by the mouse cursor (or {@link BotItemStack#EMPTY})
     */
    public BotInventory(int containerId,
                        int stateId,
                        @NotNull BotItemStack @NotNull [] slots,
                        @NotNull BotItemStack cursorItem) {
        this.containerId = containerId;
        this.stateId = stateId;
        this.slots = slots.clone();
        this.cursorItem = Objects.requireNonNull(cursorItem, "cursorItem");
    }

    /** @return the protocol container id (0 = player inventory). */
    public int containerId() { return containerId; }

    /** @return the inventory state id, used to sign outgoing click packets. */
    public int stateId() { return stateId; }

    /** @return the slot snapshot, indexed by protocol slot number. */
    public @NotNull BotItemStack @NotNull [] slots() {
        return slots.clone();
    }

    /** @return the stack held by the mouse cursor, or {@link BotItemStack#EMPTY} if none. */
    public @NotNull BotItemStack cursorItem() { return cursorItem; }

    /**
     * @param slot the protocol slot index
     * @return the snapshot for that slot, never {@code null}; {@link BotItemStack#EMPTY} if empty
     */
    public @NotNull BotItemStack slot(int slot) {
        if (slot < 0 || slot >= slots.length) return BotItemStack.EMPTY;
        BotItemStack s = slots[slot];
        return s != null ? s : BotItemStack.EMPTY;
    }

    /**
     * Convenience: scans forward for the first slot holding the given item id.
     *
     * @param itemId the raw item id to look for
     * @return the first matching slot index, or empty if none
     */
    public @NotNull Optional<Integer> first(int itemId) {
        for (int i = 0; i < slots.length; i++) {
            BotItemStack s = slots[i];
            if (s != null && s.id() == itemId && !s.isEmpty()) return Optional.of(i);
        }
        return Optional.empty();
    }

    /**
     * Convenience: scans forward for the first slot holding any non-empty stack.
     *
     * @return the slot index of the first non-empty slot, or empty if all slots are empty
     */
    public @NotNull Optional<Integer> firstNonEmpty() {
        for (int i = 0; i < slots.length; i++) {
            BotItemStack s = slots[i];
            if (s != null && !s.isEmpty()) return Optional.of(i);
        }
        return Optional.empty();
    }

    /**
     * Convenience: total count of items matching the given id, summed across all slots.
     *
     * @param itemId the item id to count
     * @return the total count, or {@code 0} if no slot holds the item
     */
    public int countOf(int itemId) {
        int total = 0;
        for (BotItemStack s : slots) {
            if (s != null && s.id() == itemId) total += s.amount();
        }
        return total;
    }

    /**
     * @return a defensive copy of just the non-empty slots, in slot order
     */
    public @NotNull List<BotItemStack> nonEmptySlots() {
        java.util.List<BotItemStack> out = new java.util.ArrayList<>();
        for (BotItemStack s : slots) {
            if (s != null && !s.isEmpty()) out.add(s);
        }
        return java.util.Collections.unmodifiableList(out);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotInventory that)) return false;
        return containerId == that.containerId
                && stateId == that.stateId
                && java.util.Arrays.equals(slots, that.slots)
                && Objects.equals(cursorItem, that.cursorItem);
    }

    @Override
    public int hashCode() {
        int r = Objects.hash(containerId, stateId, cursorItem);
        r = 31 * r + java.util.Arrays.hashCode(slots);
        return r;
    }
}
