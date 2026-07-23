package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Predicate-style matcher used by {@link Bot#awaitInventory(int, InventoryMatcher, long)}.
 *
 * <p>Static factory methods cover the most common queries; tests with custom
 * logic can implement the interface directly.</p>
 */
@FunctionalInterface
public interface InventoryMatcher extends Predicate<@NotNull BotInventory> {

    /**
     * Matches when the inventory contains at least {@code atLeast} items of the
     * given raw item id, summed across all slots.
     */
    static @NotNull InventoryMatcher hasAtLeast(int itemId, int atLeast) {
        int atLeastReq = Math.max(0, atLeast);
        return inv -> inv.countOf(itemId) >= atLeastReq;
    }

    /** Matches when the inventory contains at least one of the given raw item id. */
    static @NotNull InventoryMatcher hasItem(int itemId) {
        return hasAtLeast(itemId, 1);
    }

    /** Matches when the inventory contains no items of the given raw item id. */
    static @NotNull InventoryMatcher lacksItem(int itemId) {
        return inv -> inv.countOf(itemId) == 0;
    }

    /** Matches when a specific slot index holds a non-empty stack. */
    static @NotNull InventoryMatcher slotNonEmpty(int slot) {
        return inv -> !inv.slot(slot).isEmpty();
    }

    /** Matches when a specific slot index holds an empty stack. */
    static @NotNull InventoryMatcher slotEmpty(int slot) {
        return inv -> inv.slot(slot).isEmpty();
    }

    /** Matches when the cursor is carrying any item. */
    static @NotNull InventoryMatcher cursorNonEmpty() {
        return inv -> !inv.cursorItem().isEmpty();
    }

    /** Matches when the cursor is empty. */
    static @NotNull InventoryMatcher cursorEmpty() {
        return inv -> inv.cursorItem().isEmpty();
    }

    /** Matches when the open container id equals the given id. */
    static @NotNull InventoryMatcher containerIdEquals(int expectedId) {
        return inv -> inv.containerId() == expectedId;
    }

    /**
     * Combines this matcher with another; both must pass.
     */
    default @NotNull InventoryMatcher and(@NotNull InventoryMatcher other) {
        Objects.requireNonNull(other, "other");
        return inv -> test(inv) && other.test(inv);
    }

    /**
     * Combines this matcher with another; either may pass.
     */
    default @NotNull InventoryMatcher or(@NotNull InventoryMatcher other) {
        Objects.requireNonNull(other, "other");
        return inv -> test(inv) || other.test(inv);
    }

    /**
     * Negates this matcher.
     */
    default @NotNull InventoryMatcher negate() {
        return inv -> !test(inv);
    }
}
