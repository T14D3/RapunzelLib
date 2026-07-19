package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.ClickContext;
import de.t14d3.rapunzellib.nbt.item.RItem;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ButtonBuilder {
    private Component label;
    private Component[] tooltip;
    private Icon icon;
    private Consumer<ClickContext> onClick;
    private boolean enabled = true;
    private boolean glow = false;
    
    @NotNull
    public ButtonBuilder label(@NotNull Component label) {
        this.label = label;
        return this;
    }
    
    @NotNull
    public ButtonBuilder label(@NotNull String label) {
        return label(Component.text(label));
    }

    @NotNull
    public ButtonBuilder tooltip(@NotNull Component... tooltip) {
        this.tooltip = tooltip;
        return this;
    }
    
    @NotNull
    public ButtonBuilder tooltip(@Nullable String tooltip) {
        return tooltip(tooltip != null ? Component.text(tooltip) : null);
    }
    
    @NotNull
    public ButtonBuilder icon(@Nullable Icon icon) {
        this.icon = icon;
        return this;
    }
    
    @NotNull
    public ButtonBuilder icon(@NotNull String itemId) {
        return icon(Icon.item(itemId));
    }

    @NotNull
    public ButtonBuilder icon(@NotNull RItem item) {
        return icon(Icon.item(item));
    }
    
    @NotNull
    public ButtonBuilder onClick(@Nullable Consumer<ClickContext> onClick) {
        this.onClick = onClick;
        return this;
    }
    
    @NotNull
    public ButtonBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets whether the button's icon should render with an enchanted glint.
     *
     * <p>Useful for highlighting entries that have a non-default state, e.g. a
     * permission value that is currently allowed or denied.</p>
     *
     * @param glow {@code true} to render the glint
     * @return this builder
     */
    @NotNull
    public ButtonBuilder glow(boolean glow) {
        this.glow = glow;
        return this;
    }
    
    @NotNull
    public ButtonElement build() {
        if (label == null) {
            throw new IllegalStateException("Label is required");
        }
        
        return new ButtonElement() {
            @Override
            public @NotNull Component label() {
                return label;
            }
            
            @Override
            public @NotNull Component[] tooltip() {
                return tooltip;
            }
            
            @Override
            public @Nullable Icon icon() {
                return icon;
            }
            
            @Override
            public @Nullable Consumer<ClickContext> onClick() {
                return onClick;
            }
            
            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public boolean glow() {
                return glow;
            }
        };
    }
}
