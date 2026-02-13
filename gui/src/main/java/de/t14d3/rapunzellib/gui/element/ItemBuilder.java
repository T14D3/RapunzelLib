package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.ClickContext;
import de.t14d3.rapunzellib.nbt.item.RItem;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ItemBuilder {
    private RItem item;
    private Component tooltip;
    private Consumer<ClickContext> onClick;
    
    @NotNull
    public ItemBuilder item(@NotNull RItem item) {
        this.item = item;
        return this;
    }
    
    @NotNull
    public ItemBuilder tooltip(@Nullable Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }
    
    @NotNull
    public ItemBuilder tooltip(@Nullable String tooltip) {
        return tooltip(tooltip != null ? Component.text(tooltip) : null);
    }
    
    @NotNull
    public ItemBuilder onClick(@Nullable Consumer<ClickContext> onClick) {
        this.onClick = onClick;
        return this;
    }
    
    @NotNull
    public ItemElement build() {
        if (item == null) {
            throw new IllegalStateException("Item is required");
        }
        
        return new ItemElement() {
            @Override
            public @NotNull RItem item() {
                return item;
            }
            
            @Override
            public @Nullable Component tooltip() {
                return tooltip;
            }
            
            @Override
            public @Nullable Consumer<ClickContext> onClick() {
                return onClick;
            }
        };
    }
}
