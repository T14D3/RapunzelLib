package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.context.DropdownContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DropdownBuilder {
    private String key;
    private Component label;
    private final List<Option> options = new ArrayList<>();
    private Option defaultValue;
    private Consumer<DropdownContext> onChange;
    
    @NotNull
    public DropdownBuilder key(@NotNull String key) {
        this.key = key;
        return this;
    }
    
    @NotNull
    public DropdownBuilder label(@Nullable Component label) {
        this.label = label;
        return this;
    }
    
    @NotNull
    public DropdownBuilder label(@Nullable String label) {
        return label(label != null ? Component.text(label) : null);
    }
    
    @NotNull
    public DropdownBuilder option(@NotNull Option option) {
        this.options.add(option);
        return this;
    }
    
    @NotNull
    public DropdownBuilder option(@NotNull String id, @NotNull Component display) {
        return option(Option.of(id, display));
    }
    
    @NotNull
    public DropdownBuilder option(@NotNull String id, @NotNull String display) {
        return option(Option.of(id, display));
    }
    
    @NotNull
    public DropdownBuilder options(@NotNull List<Option> options) {
        this.options.addAll(options);
        return this;
    }
    
    @NotNull
    public DropdownBuilder defaultValue(@Nullable Option defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
    
    @NotNull
    public DropdownBuilder defaultValueById(@Nullable String id) {
        if (id == null) {
            this.defaultValue = null;
            return this;
        }
        for (Option opt : options) {
            if (opt.id().equals(id)) {
                this.defaultValue = opt;
                break;
            }
        }
        return this;
    }
    
    @NotNull
    public DropdownBuilder onChange(@Nullable Consumer<DropdownContext> onChange) {
        this.onChange = onChange;
        return this;
    }
    
    @NotNull
    public DropdownElement build() {
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("Key is required");
        }
        
        return new DropdownElement() {
            @Override
            public @NotNull String key() {
                return key;
            }
            
            @Override
            public @Nullable Component label() {
                return label;
            }
            
            @Override
            public @NotNull List<Option> options() {
                return List.copyOf(options);
            }
            
            @Override
            public @Nullable Option defaultValue() {
                return defaultValue;
            }
            
            @Override
            public @Nullable Consumer<DropdownContext> onChange() {
                return onChange;
            }
        };
    }
}
