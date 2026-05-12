package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Post-event fired after a player has opened an inventory.
 */
public final class InventoryOpenPost implements GamePostEvent {
    private final RPlayer player;
    private final RInventory inventory;

    /**
     * Creates a new InventoryOpenPost event.
     *
     * @param player    the player who opened the inventory
     * @param inventory the inventory that was opened
     */
    public InventoryOpenPost(@NotNull RPlayer player, @NotNull RInventory inventory) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    /**
     * Returns the player who opened the inventory.
     *
     * @return the player
     */
    public @NotNull RPlayer player() {
        return player;
    }

    /**
     * Returns the inventory that was opened.
     *
     * @return the inventory
     */
    public @NotNull RInventory inventory() {
        return inventory;
    }
}
