package de.t14d3.rapunzellib.gui.element;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class TextBuilder {
    private Component text;
    private boolean multiline;
    
    @NotNull
    public TextBuilder text(@NotNull Component text) {
        this.text = text;
        return this;
    }
    
    @NotNull
    public TextBuilder text(@NotNull String text) {
        return text(Component.text(text));
    }
    
    @NotNull
    public TextBuilder multiline(boolean multiline) {
        this.multiline = multiline;
        return this;
    }
    
    @NotNull
    public TextElement build() {
        if (text == null) {
            throw new IllegalStateException("Text is required");
        }
        
        return new TextElement() {
            @Override
            public @NotNull Component text() {
                return text;
            }
            
            @Override
            public boolean multiline() {
                return multiline;
            }
        };
    }
}
