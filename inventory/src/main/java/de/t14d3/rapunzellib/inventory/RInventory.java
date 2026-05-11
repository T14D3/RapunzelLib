package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RContainer;
import de.t14d3.rapunzellib.objects.RKey;
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

    default @NotNull Optional<Integer> firstEmpty() {
        int size = size();
        for (int slot = 0; slot < size; slot++) {
            if (item(slot).isEmpty()) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

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
