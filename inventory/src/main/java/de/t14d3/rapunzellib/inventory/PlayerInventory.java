package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Provides access to the sub-inventories of a Minecraft player: the main inventory,
 * armor slots, and ender chest.
 * <p>
 * Convenience methods delegate common item operations (has space, pickup, add, remove)
 * to the main player inventory.
 */
public interface PlayerInventory {

    @NotNull RInventory inventory();

    /**
     * Returns the armor inventory (helmet, chestplate, leggings, boots).
     *
     * @return the armor inventory
     */
    @NotNull RInventory armor();

    @NotNull RInventory enderChest();

    /**
     * Checks whether the main inventory has at least one empty slot.
     *
     * @param item the item to consider (used for future filtering if needed)
     * @return true if there is an empty slot available
     */
    default boolean hasSpaceFor(@NotNull RItem item) {
        return inventory().firstEmpty().isPresent();
    }

    /**
     * Checks whether the player can pick up the given item (alias for {@link #hasSpaceFor}).
     *
     * @param item the item to check
     * @return true if the item can be picked up
     */
    default boolean canPickup(@NotNull RItem item) {
        return hasSpaceFor(item);
    }

    /**
     * Adds the given item to the main inventory.
     *
     * @param item the item to add
     * @return true if the entire stack was added successfully
     */
    default boolean addItem(@NotNull RItem item) {
        return inventory().addItem(item).isEmpty();
    }

    /**
     * Removes up to the specified amount of the given material from the main inventory.
     *
     * @param material the material to remove
     * @param amount   the maximum amount to remove
     * @return an {@link Optional} containing the removed items if the full amount
     *         could not be satisfied, or empty if the full amount was removed
     */
    default @NotNull Optional<RItem> removeItem(@NotNull RKey material, int amount) {
        return inventory().removeItem(material, amount);
    }
}
