package de.t14d3.rapunzellib.gui.element;

import org.jetbrains.annotations.NotNull;

public interface DividerElement extends GuiElement {
    boolean isVertical();
    
    @Override
    default @NotNull ElementType type() {
        return ElementType.DIVIDER;
    }
    
    @NotNull
    static DividerElement horizontal() {
        return new DividerElement() {
            @Override
            public boolean isVertical() {
                return false;
            }
        };
    }
    
    @NotNull
    static DividerElement vertical() {
        return new DividerElement() {
            @Override
            public boolean isVertical() {
                return true;
            }
        };
    }
}
