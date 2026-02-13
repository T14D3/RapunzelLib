package de.t14d3.rapunzellib.gui.element;

import org.jetbrains.annotations.NotNull;

public sealed interface Icon permits Icon.ItemIcon, Icon.CustomIcon, Icon.NoneIcon {
    @NotNull
    static Icon item(@NotNull String itemId) {
        return new ItemIcon(itemId);
    }
    
    @NotNull
    static Icon custom(@NotNull String id) {
        return new CustomIcon(id);
    }
    
    @NotNull
    static Icon none() {
        return new NoneIcon();
    }

    record ItemIcon(@NotNull String itemId) implements Icon {
    }

    record CustomIcon(@NotNull String id) implements Icon {
    }

    record NoneIcon() implements Icon {
    }
}
