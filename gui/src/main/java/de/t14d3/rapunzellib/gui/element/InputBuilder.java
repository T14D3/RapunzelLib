package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.InputContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class InputBuilder {
    private String key;
    private Component label;
    private String placeholder;
    private String defaultValue;
    private int maxLength = 256;
    private Consumer<InputContext> onChange;
    
    @NotNull
    public InputBuilder key(@NotNull String key) {
        this.key = key;
        return this;
    }
    
    @NotNull
    public InputBuilder label(@Nullable Component label) {
        this.label = label;
        return this;
    }
    
    @NotNull
    public InputBuilder label(@Nullable String label) {
        return label(label != null ? Component.text(label) : null);
    }
    
    @NotNull
    public InputBuilder placeholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }
    
    @NotNull
    public InputBuilder defaultValue(@Nullable String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
    
    @NotNull
    public InputBuilder maxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }
    
    @NotNull
    public InputBuilder onChange(@Nullable Consumer<InputContext> onChange) {
        this.onChange = onChange;
        return this;
    }
    
    @NotNull
    public InputElement build() {
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("Key is required");
        }
        
        return new InputElement() {
            @Override
            public @NotNull String key() {
                return key;
            }
            
            @Override
            public @Nullable Component label() {
                return label;
            }
            
            @Override
            public @Nullable String placeholder() {
                return placeholder;
            }
            
            @Override
            public @Nullable String defaultValue() {
                return defaultValue;
            }
            
            @Override
            public int maxLength() {
                return maxLength;
            }
            
            @Override
            public @Nullable Consumer<InputContext> onChange() {
                return onChange;
            }
        };
    }
}
