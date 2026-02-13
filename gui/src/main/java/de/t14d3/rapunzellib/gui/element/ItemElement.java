package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.ClickContext;
import de.t14d3.rapunzellib.nbt.item.RItem;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface ItemElement extends GuiElement {
    @NotNull RItem item();
    
    @Nullable Component tooltip();
    
    @Nullable Consumer<ClickContext> onClick();
    
    @Override
    default @NotNull ElementType type() {
        return ElementType.ITEM;
    }
    
    @NotNull
    static ItemElement of(@NotNull RItem item) {
        return builder().item(item).build();
    }
    
    @NotNull
    static ItemBuilder builder() {
        return new ItemBuilder();
    }
}
