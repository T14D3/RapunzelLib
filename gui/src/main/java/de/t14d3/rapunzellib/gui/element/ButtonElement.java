package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.ClickContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface ButtonElement extends GuiElement {
    @NotNull Component label();
    
    @NotNull Component[] tooltip();
    
    @Nullable Icon icon();
    
    @Nullable Consumer<ClickContext> onClick();
    
    default boolean enabled() {
        return true;
    }
    
    @Override
    default @NotNull ElementType type() {
        return ElementType.BUTTON;
    }
    
    @NotNull
    static ButtonElement of(@NotNull Component label) {
        return builder().label(label).tooltip(Component.empty()).build();
    }
    
    @NotNull
    static ButtonBuilder builder() {
        return new ButtonBuilder();
    }
}
