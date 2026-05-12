package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Pre-event fired before a player opens an inventory.
 *
 * <p>This event is cancellable. If denied, the inventory will not be opened.</p>
 */
public final class InventoryOpenPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RInventory inventory;

    /**
     * Creates a new InventoryOpenPre event.
     *
     * @param player    the player opening the inventory
     * @param inventory the inventory being opened
     */
    public InventoryOpenPre(@NotNull RPlayer player, @NotNull RInventory inventory) {
        this(player, inventory, false);
    }

    /**
     * Creates a new InventoryOpenPre event with cancelled state.
     *
     * @param player    the player opening the inventory
     * @param inventory the inventory being opened
     * @param cancelled whether the event is initially cancelled
     */
    public InventoryOpenPre(@NotNull RPlayer player, @NotNull RInventory inventory, boolean cancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        setCancelled(cancelled);
    }

    /**
     * Returns the player opening the inventory.
     *
     * @return the player
     */
    public @NotNull RPlayer player() {
        return player;
    }

    /**
     * Returns the inventory being opened.
     *
     * @return the inventory
     */
    public @NotNull RInventory inventory() {
        return inventory;
    }
}
