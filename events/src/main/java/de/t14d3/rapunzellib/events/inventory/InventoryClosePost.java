package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Post-event fired after a player has closed an inventory.
 */
public final class InventoryClosePost implements GamePostEvent {
    private final RPlayer player;
    private final RInventory inventory;

    /**
     * Creates a new InventoryClosePost event.
     *
     * @param player    the player who closed the inventory
     * @param inventory the inventory that was closed
     */
    public InventoryClosePost(@NotNull RPlayer player, @NotNull RInventory inventory) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    /**
     * Returns the player who closed the inventory.
     *
     * @return the player
     */
    public @NotNull RPlayer player() {
        return player;
    }

    /**
     * Returns the inventory that was closed.
     *
     * @return the inventory
     */
    public @NotNull RInventory inventory() {
        return inventory;
    }
}
