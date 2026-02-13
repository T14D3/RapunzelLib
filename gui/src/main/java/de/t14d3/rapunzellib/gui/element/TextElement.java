package de.t14d3.rapunzellib.gui.element;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface TextElement extends GuiElement {
    @NotNull Component text();
    
    default boolean multiline() {
        return false;
    }
    
    @Override
    default @NotNull ElementType type() {
        return ElementType.TEXT;
    }
    
    @NotNull
    static TextElement of(@NotNull Component text) {
        return builder().text(text).build();
    }
    
    @NotNull
    static TextBuilder builder() {
        return new TextBuilder();
    }
}
