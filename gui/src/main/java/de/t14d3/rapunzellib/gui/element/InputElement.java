package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.InputContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface InputElement extends GuiElement {
    @NotNull String key();
    
    @Nullable Component label();
    
    @Nullable String placeholder();
    
    @Nullable String defaultValue();
    
    default int maxLength() {
        return 256;
    }
    
    @Nullable Consumer<InputContext> onChange();
    
    @Override
    default @NotNull ElementType type() {
        return ElementType.INPUT;
    }
    
    @NotNull
    static InputElement of(@NotNull String key) {
        return builder().key(key).build();
    }
    
    @NotNull
    static InputBuilder builder() {
        return new InputBuilder();
    }
}
