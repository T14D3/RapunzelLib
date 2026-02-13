package de.t14d3.rapunzellib.gui.layout;

import de.t14d3.rapunzellib.gui.element.GuiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface LinearLayout extends GuiLayout {
    @NotNull List<GuiElement> elements();
    
    default boolean isVertical() {
        return true;
    }
    
    @NotNull
    static Builder builder() {
        return new Builder();
    }
    
    @NotNull
    static Builder horizontal() {
        return new Builder().vertical(false);
    }
    
    @NotNull
    static Builder vertical() {
        return new Builder().vertical(true);
    }
    
    class Builder {
        private final List<GuiElement> elements = new ArrayList<>();
        private boolean vertical = true;
        
        @NotNull
        public Builder vertical(boolean vertical) {
            this.vertical = vertical;
            return this;
        }
        
        @NotNull
        public Builder element(@NotNull GuiElement element) {
            this.elements.add(element);
            return this;
        }
        
        @NotNull
        public Builder elements(@NotNull GuiElement... elements) {
            for (GuiElement element : elements) {
                this.elements.add(element);
            }
            return this;
        }
        
        @NotNull
        public Builder elements(@NotNull List<GuiElement> elements) {
            this.elements.addAll(elements);
            return this;
        }
        
        @NotNull
        public LinearLayout build() {
            List<GuiElement> finalElements = List.copyOf(elements);
            boolean finalVertical = vertical;
            
            return new LinearLayout() {
                @Override
                public @NotNull List<GuiElement> elements() {
                    return finalElements;
                }
                
                @Override
                public boolean isVertical() {
                    return finalVertical;
                }
            };
        }
    }
}
