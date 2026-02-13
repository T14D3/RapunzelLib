package de.t14d3.rapunzellib.gui.element;

import org.jetbrains.annotations.NotNull;

public interface SpacerElement extends GuiElement {
    @Override
    int width();
    
    @Override
    int height();
    
    @Override
    default @NotNull ElementType type() {
        return ElementType.SPACER;
    }
    
    @NotNull
    static SpacerElement of(int width, int height) {
        return new SpacerElement() {
            @Override
            public int width() {
                return width;
            }
            
            @Override
            public int height() {
                return height;
            }
        };
    }
    
    @NotNull
    static SpacerElement width(int width) {
        return of(width, 1);
    }
    
    @NotNull
    static SpacerElement height(int height) {
        return of(1, height);
    }
}
