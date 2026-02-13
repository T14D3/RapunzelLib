package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class InventoryOpenPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RInventory inventory;

    public InventoryOpenPre(@NotNull RPlayer player, @NotNull RInventory inventory) {
        this(player, inventory, false);
    }

    public InventoryOpenPre(@NotNull RPlayer player, @NotNull RInventory inventory, boolean cancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        setCancelled(cancelled);
    }

    public @NotNull RPlayer player() {
        return player;
    }

    public @NotNull RInventory inventory() {
        return inventory;
    }
}
