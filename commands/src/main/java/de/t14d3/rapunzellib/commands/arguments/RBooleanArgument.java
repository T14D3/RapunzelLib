package de.t14d3.rapunzellib.commands.arguments;

import com.mojang.brigadier.arguments.BoolArgumentType;
import de.t14d3.rapunzellib.commands.RCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Boolean argument type for command parsing.
 * <p>
 * Parses boolean values (true/false) with optional default values.
 * Supports tab completion with "true" and "false" suggestions.
 * </p>
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * // Required boolean argument
 * RBooleanArgument<RCommandSource> enabled = RBooleanArgument.required("enabled");
 * 
 * // Optional boolean with default value
 * RBooleanArgument<RCommandSource> verbose = RBooleanArgument.optional("verbose", false);
 * }</pre>
 * 
 * @param <S> the command source type
 */
public class RBooleanArgument<S extends RCommandSource> implements RArgument<S, Boolean> {
    
    private static final List<String> SUGGESTIONS = Arrays.asList("true", "false");
    
    private final String name;
    private final boolean optional;
    private final Supplier<Boolean> defaultValue;
    
    private RBooleanArgument(Builder<S> builder) {
        this.name = builder.name;
        this.optional = builder.optional;
        this.defaultValue = builder.defaultValue;
    }
    
    public static <S extends RCommandSource> RBooleanArgument<S> required(@NotNull String name) {
        return new Builder<S>(name)
            .optional(false)
            .build();
    }
    
    public static <S extends RCommandSource> RBooleanArgument<S> optional(@NotNull String name, boolean defaultValue) {
        return new Builder<S>(name)
            .optional(true)
            .defaultValue(defaultValue)
            .build();
    }
    
    @NotNull
    @Override
    public String getName() {
        return name;
    }
    
    @NotNull
    @Override
    public BoolArgumentType getArgumentType() {
        return BoolArgumentType.bool();
    }
    
    @Override
    public boolean isOptional() {
        return optional;
    }
    
    @Nullable
    @Override
    public Supplier<Boolean> getDefaultValue() {
        return defaultValue;
    }
    
    @NotNull
    @Override
    public List<String> getSuggestions(@NotNull S source) {
        return SUGGESTIONS;
    }
    
    @Override
    public Boolean parse(@NotNull String input) throws IllegalArgumentException {
        try {
            return Boolean.parseBoolean(input);
        } catch (Exception e) {
            throw new IllegalArgumentException("'" + input + "' is not a valid boolean. Use 'true' or 'false'.");
        }
    }
    
    /**
     * Builder for creating boolean arguments.
     */
    public static class Builder<S extends RCommandSource> {
        private final String name;
        private boolean optional = false;
        private Supplier<Boolean> defaultValue = null;
        
        public Builder(@NotNull String name) {
            this.name = name;
        }
        
        public Builder<S> optional(boolean optional) {
            this.optional = optional;
            return this;
        }
        
        public Builder<S> defaultValue(boolean value) {
            this.defaultValue = () -> value;
            return this;
        }
        
        public RBooleanArgument<S> build() {
            return new RBooleanArgument<>(this);
        }
    }
}
