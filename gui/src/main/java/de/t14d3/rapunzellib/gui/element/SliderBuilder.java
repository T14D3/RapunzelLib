package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.SliderContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class SliderBuilder {
    private String key;
    private Component label;
    private float min = 0.0f;
    private float max = 100.0f;
    private float step = 1.0f;
    private float defaultValue;
    private String format = "%s: %s";
    private Consumer<SliderContext> onChange;
    
    @NotNull
    public SliderBuilder key(@NotNull String key) {
        this.key = key;
        return this;
    }
    
    @NotNull
    public SliderBuilder label(@Nullable Component label) {
        this.label = label;
        return this;
    }
    
    @NotNull
    public SliderBuilder label(@Nullable String label) {
        return label(label != null ? Component.text(label) : null);
    }
    
    @NotNull
    public SliderBuilder min(float min) {
        this.min = min;
        return this;
    }
    
    @NotNull
    public SliderBuilder max(float max) {
        this.max = max;
        return this;
    }
    
    @NotNull
    public SliderBuilder step(float step) {
        this.step = step;
        return this;
    }
    
    @NotNull
    public SliderBuilder range(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }
    
    @NotNull
    public SliderBuilder defaultValue(float defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @NotNull
    public SliderBuilder format(String format) {
        this.format = format;
        return this;
    }
    
    @NotNull
    public SliderBuilder onChange(@Nullable Consumer<SliderContext> onChange) {
        this.onChange = onChange;
        return this;
    }
    
    @NotNull
    public SliderElement build() {
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("Key is required");
        }
        
        return new SliderElement() {
            @Override
            public @NotNull String key() {
                return key;
            }
            
            @Override
            public @Nullable Component label() {
                return label;
            }
            
            @Override
            public float min() {
                return min;
            }
            
            @Override
            public float max() {
                return max;
            }
            
            @Override
            public float step() {
                return step;
            }

            @Override
            public String format() {
                return format;
            }
            
            @Override
            public float defaultValue() {
                return defaultValue;
            }
            
            @Override
            public @Nullable Consumer<SliderContext> onChange() {
                return onChange;
            }
        };
    }
}
