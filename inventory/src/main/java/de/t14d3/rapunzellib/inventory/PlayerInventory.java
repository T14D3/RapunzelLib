package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface PlayerInventory {
    @NotNull RInventory inventory();

    @NotNull RInventory armor();

    @NotNull RInventory enderChest();

    default boolean hasSpaceFor(@NotNull RItem item) {
        return inventory().firstEmpty().isPresent();
    }

    default boolean canPickup(@NotNull RItem item) {
        return hasSpaceFor(item);
    }

    default boolean addItem(@NotNull RItem item) {
        return inventory().addItem(item).isEmpty();
    }

    default @NotNull Optional<RItem> removeItem(@NotNull RKey material, int amount) {
        return inventory().removeItem(material, amount);
    }
}
