package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.nbt.item.RItem;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public sealed interface Icon permits Icon.ItemIcon, Icon.CustomIcon, Icon.NoneIcon {
    @NotNull
    static Icon item(@NotNull String itemId) {
        return item(RItem.of(normalizeItemKey(itemId)));
    }

    @NotNull
    static Icon item(@NotNull RItem item) {
        return new ItemIcon(Objects.requireNonNull(item, "item"));
    }
    
    @NotNull
    static Icon custom(@NotNull String id) {
        return new CustomIcon(id);
    }
    
    @NotNull
    static Icon none() {
        return new NoneIcon();
    }

    record ItemIcon(@NotNull RItem item) implements Icon {
    }

    record CustomIcon(@NotNull String id) implements Icon {
    }

    record NoneIcon() implements Icon {
    }

    private static @NotNull String normalizeItemKey(@NotNull String itemKey) {
        String trimmed = Objects.requireNonNull(itemKey, "itemKey").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("itemKey cannot be blank");
        }
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }
}
