package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface RInventory extends RContainer {
    int size();

    @NotNull Optional<RItem> item(int slot);

    void setItem(int slot, @Nullable RItem item);

    default @NotNull List<Optional<RItem>> contents() {
        int size = size();
        List<Optional<RItem>> contents = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) {
            contents.add(item(slot));
        }
        return List.copyOf(contents);
    }

    default void clear() {
        int size = size();
        for (int slot = 0; slot < size; slot++) {
            setItem(slot, null);
        }
    }
}
