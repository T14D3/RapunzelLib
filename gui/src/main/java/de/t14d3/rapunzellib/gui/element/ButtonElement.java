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

    /**
     * Whether the button's icon should render with an enchanted glint override.
     *
     * <p>Defaults to {@code false}. When {@code true}, the platform renderer applies
     * an enchantment glint to the displayed item (via {@code enchantment_glint_override}
     * on modern Minecraft versions, or a hidden enchantment on legacy versions).</p>
     *
     * @return {@code true} if the button should glow
     */
    default boolean glow() {
        return false;
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
