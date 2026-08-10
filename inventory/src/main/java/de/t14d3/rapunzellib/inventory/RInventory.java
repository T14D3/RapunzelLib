package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RContainer;
import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a generic slot-based inventory with item query, mutation, and bulk operations.
 * <p>
 * Implementations provide access to the underlying platform inventory via slot indices.
 * Default methods offer convenience for content listing, clearing, adding, and removing items.
 */
public interface RInventory extends RContainer {

    int size();

    /**
     * Returns the raw slot index where the player's own inventory section
     * starts in this (combined) inventory.
     *
     * <p>Only meaningful for wraps of a FULL combined menu - the top
     * container plus the player inventory section - such as the ones exposed
     * by {@code InventoryActionPre#inventory()} /
     * {@code InventoryActionPost#inventory()}. Raw slots below the returned
     * index belong to the top container, raw slots at or beyond it belong to
     * the player's own inventory (the "bottom" section, e.g. Bukkit's
     * {@code InventoryClickEvent#getClickedInventory()} being the player
     * inventory). This is the canonical way to distinguish the top section
     * from the player section of a menu payload.</p>
     *
     * <p>For vanilla container menus the player section always spans the
     * last 36 slots (27 main inventory + 9 hotbar), so the default
     * implementation returns {@code size() - 36}. Wrappers that know the
     * platform view may override this with an exact computation - e.g. the
     * Paper bridge computes it from the Bukkit view's top inventory size,
     * which also covers the player's own CRAFTING view (where the player
     * section starts before {@code size() - 36} because armor and the
     * offhand are part of it).</p>
     *
     * <p>For wraps that do not cover a combined menu (plain containers,
     * single inventories) this value is not defined; consumers must only
     * call it on full-menu wraps.</p>
     *
     * @return the raw slot index of the first player-inventory slot, or
     *         {@code size() - 36} when the platform-specific start is unknown
     */
    default int playerInventoryStart() {
        return size() - 36;
    }

    /**
     * Retrieves the item in the specified slot, if any.
     *
     * @param slot the slot index (0-based)
     * @return an {@link Optional} containing the item, or empty if the slot is empty
     */
    @NotNull Optional<RItem> item(int slot);

    /**
     * Replaces the item in the specified slot.
     *
     * @param slot the slot index (0-based)
     * @param item the item to place, or {@code null} to clear the slot
     */
    void setItem(int slot, @Nullable RItem item);

    default @NotNull List<Optional<RItem>> contents() {
        int size = size();
        List<Optional<RItem>> contents = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) {
            contents.add(item(slot));
        }
        return List.copyOf(contents);
    }

    /**
     * Clears every slot in this inventory by setting each to {@code null}.
     */
    default void clear() {
        int size = size();
        for (int slot = 0; slot < size; slot++) {
            setItem(slot, null);
        }
    }

    /**
     * Finds the first empty slot in this inventory.
     *
     * @return an {@link Optional} containing the first empty slot index, or empty if the inventory is full
     */
    default @NotNull Optional<Integer> firstEmpty() {
        int size = size();
        for (int slot = 0; slot < size; slot++) {
            if (item(slot).isEmpty()) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    /**
     * Attempts to add the given item to this inventory, stacking with existing similar items
     * and filling empty slots as needed.
     *
     * @param item the item to add (must not be null)
     * @return an {@link Optional} containing the remainder that could not be added, or empty if all was added
     */
    default @NotNull Optional<RItem> addItem(@NotNull RItem item) {
        if (item == null || item.isEmpty()) {
            return Optional.ofNullable(item);
        }
        int size = size();
        int remaining = item.count();
        for (int slot = 0; slot < size && remaining > 0; slot++) {
            Optional<RItem> current = item(slot);
            if (current.isEmpty()) {
                int add = Math.min(remaining, item.maxStackSize());
                setItem(slot, item.withCount(add));
                remaining -= add;
            } else if (current.get().isSimilar(item)) {
                int canAdd = Math.min(item.maxStackSize() - current.get().count(), remaining);
                if (canAdd > 0) {
                    setItem(slot, current.get().withCount(current.get().count() + canAdd));
                    remaining -= canAdd;
                }
            }
        }
        if (remaining > 0) {
            return Optional.of(item.withCount(remaining));
        }
        return Optional.empty();
    }

    /**
     * Removes up to the specified amount of items matching the given material from this inventory.
     *
     * @param material the material key to match
     * @param amount   the maximum number of items to remove
     * @return an {@link Optional} containing the removed items if the full amount could not be satisfied,
     *         or empty if the requested amount was fully removed
     */
    default @NotNull Optional<RItem> removeItem(@NotNull RKey material, int amount) {
        if (material == null || amount <= 0) {
            return Optional.empty();
        }
        int size = size();
        int remaining = amount;
        for (int slot = 0; slot < size && remaining > 0; slot++) {
            Optional<RItem> current = item(slot);
            if (current.isPresent()) {
                RItem item = current.get();
                if (item.material().equals(material)) {
                    int remove = Math.min(remaining, item.count());
                    if (remove == item.count()) {
                        setItem(slot, null);
                    } else {
                        setItem(slot, item.withCount(item.count() - remove));
                    }
                    remaining -= remove;
                }
            }
        }
        if (remaining > 0) {
            return Optional.of(RItem.of(material, amount - remaining));
        }
        return Optional.empty();
    }
}
