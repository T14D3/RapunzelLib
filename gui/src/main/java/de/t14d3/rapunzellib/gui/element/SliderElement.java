package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.SliderContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface SliderElement extends GuiElement {
    @NotNull String key();
    
    @Nullable Component label();
    
    default float min() {
        return 0.0f;
    }
    
    default float max() {
        return 100.0f;
    }
    
    default float step() {
        return 1.0f;
    }
    
    default float defaultValue() {
        return min();
    }

    default String format() {
        return "%s: %s";
    }
    
    @Nullable Consumer<SliderContext> onChange();
    
    @Override
    default @NotNull ElementType type() {
        return ElementType.SLIDER;
    }
    
    @NotNull
    static SliderElement of(@NotNull String key) {
        return builder().key(key).build();
    }
    
    @NotNull
    static SliderBuilder builder() {
        return new SliderBuilder();
    }
}
