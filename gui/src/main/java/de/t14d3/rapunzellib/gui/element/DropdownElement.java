package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.DropdownContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public interface DropdownElement extends GuiElement {
    @NotNull String key();
    
    @Nullable Component label();
    
    default @NotNull List<Option> options() {
        return Collections.emptyList();
    }
    
    @Nullable Option defaultValue();
    
    @Nullable Consumer<DropdownContext> onChange();
    
    @Override
    default @NotNull ElementType type() {
        return ElementType.DROPDOWN;
    }
    
    @NotNull
    static DropdownElement of(@NotNull String key) {
        return builder().key(key).build();
    }
    
    @NotNull
    static DropdownBuilder builder() {
        return new DropdownBuilder();
    }
}
