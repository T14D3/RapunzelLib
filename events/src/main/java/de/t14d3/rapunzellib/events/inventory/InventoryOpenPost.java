package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class InventoryOpenPost implements GamePostEvent {
    private final RPlayer player;
    private final RInventory inventory;

    public InventoryOpenPost(@NotNull RPlayer player, @NotNull RInventory inventory) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    public @NotNull RPlayer player() {
        return player;
    }

    public @NotNull RInventory inventory() {
        return inventory;
    }
}
