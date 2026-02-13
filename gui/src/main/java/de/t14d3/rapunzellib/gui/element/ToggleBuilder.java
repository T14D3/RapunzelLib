package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.ToggleContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ToggleBuilder {
    private String key;
    private Component label;
    private boolean defaultValue;
    private Consumer<ToggleContext> onChange;
    
    @NotNull
    public ToggleBuilder key(@NotNull String key) {
        this.key = key;
        return this;
    }
    
    @NotNull
    public ToggleBuilder label(@Nullable Component label) {
        this.label = label;
        return this;
    }
    
    @NotNull
    public ToggleBuilder label(@Nullable String label) {
        return label(label != null ? Component.text(label) : null);
    }
    
    @NotNull
    public ToggleBuilder defaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
    
    @NotNull
    public ToggleBuilder onChange(@Nullable Consumer<ToggleContext> onChange) {
        this.onChange = onChange;
        return this;
    }
    
    @NotNull
    public ToggleElement build() {
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("Key is required");
        }
        
        return new ToggleElement() {
            @Override
            public @NotNull String key() {
                return key;
            }
            
            @Override
            public @Nullable Component label() {
                return label;
            }
            
            @Override
            public boolean defaultValue() {
                return defaultValue;
            }
            
            @Override
            public @Nullable Consumer<ToggleContext> onChange() {
                return onChange;
            }
        };
    }
}
