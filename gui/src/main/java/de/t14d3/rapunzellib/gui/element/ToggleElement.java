package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.ToggleContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface ToggleElement extends GuiElement {
    @NotNull String key();
    
    @Nullable Component label();
    
    default boolean defaultValue() {
        return false;
    }
    
    @Nullable Consumer<ToggleContext> onChange();
    
    @Override
    default @NotNull ElementType type() {
        return ElementType.TOGGLE;
    }
    
    @NotNull
    static ToggleElement of(@NotNull String key) {
        return builder().key(key).build();
    }
    
    @NotNull
    static ToggleBuilder builder() {
        return new ToggleBuilder();
    }
}
