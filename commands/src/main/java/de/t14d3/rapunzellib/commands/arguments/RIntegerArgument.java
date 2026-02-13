package de.t14d3.rapunzellib.commands.arguments;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import de.t14d3.rapunzellib.commands.RCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Integer argument type for command parsing.
 * <p>
 * Supports optional arguments with default values and range validation.
 * </p>
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * // Required integer argument
 * RIntegerArgument<RCommandSource> amount = RIntegerArgument.required("amount");
 * 
 * // Optional integer with default value
 * RIntegerArgument<RCommandSource> limit = RIntegerArgument.optional("limit", 10);
 * 
 * // Integer with min/max validation
 * RIntegerArgument<RCommandSource> health = new RIntegerArgument<>("health")
 *     .min(0).max(20);
 * }</pre>
 * 
 * @param <S> the command source type
 */
public class RIntegerArgument<S extends RCommandSource> implements RArgument<S, Integer> {
    
    private final String name;
    private final boolean optional;
    private final Supplier<Integer> defaultValue;
    private final IntegerArgumentType argumentType;
    private final Integer min;
    private final Integer max;
    
    private RIntegerArgument(Builder<S> builder) {
        this.name = builder.name;
        this.optional = builder.optional;
        this.defaultValue = builder.defaultValue;
        this.min = builder.min;
        this.max = builder.max;
        
        if (min != null && max != null) {
            this.argumentType = IntegerArgumentType.integer(min, max);
        } else if (min != null) {
            this.argumentType = IntegerArgumentType.integer(min);
        } else {
            this.argumentType = IntegerArgumentType.integer();
        }
    }
    
    /**
     * Creates a required integer argument.
     * 
     * @param name the argument name
     * @param <S> the command source type
     * @return a new integer argument
     */
    public static <S extends RCommandSource> RIntegerArgument<S> required(@NotNull String name) {
        return new Builder<S>(name)
            .optional(false)
            .build();
    }
    
    /**
     * Creates an optional integer argument with a default value.
     * 
     * @param name the argument name
     * @param defaultValue the default value
     * @param <S> the command source type
     * @return a new integer argument
     */
    public static <S extends RCommandSource> RIntegerArgument<S> optional(@NotNull String name, int defaultValue) {
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
    public IntegerArgumentType getArgumentType() {
        return argumentType;
    }
    
    @Override
    public boolean isOptional() {
        return optional;
    }
    
    @Nullable
    @Override
    public Supplier<Integer> getDefaultValue() {
        return defaultValue;
    }
    
    @NotNull
    @Override
    public List<String> getSuggestions(@NotNull S source) {
        return Collections.emptyList();
    }
    
    @Override
    public Integer parse(@NotNull String input) throws IllegalArgumentException {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + input + "' is not a valid integer");
        }
    }
    
    @Override
    public boolean isValid(@NotNull Integer value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }
    
    /**
     * Gets the minimum value for this argument.
     * 
     * @return the minimum value, or null if not set
     */
    @Nullable
    public Integer getMin() {
        return min;
    }
    
    /**
     * Gets the maximum value for this argument.
     * 
     * @return the maximum value, or null if not set
     */
    @Nullable
    public Integer getMax() {
        return max;
    }
    
    /**
     * Builder for creating integer arguments.
     */
    public static class Builder<S extends RCommandSource> {
        private final String name;
        private boolean optional = false;
        private Supplier<Integer> defaultValue = null;
        private Integer min = null;
        private Integer max = null;
        
        public Builder(@NotNull String name) {
            this.name = name;
        }
        
        public Builder<S> optional(boolean optional) {
            this.optional = optional;
            return this;
        }
        
        public Builder<S> defaultValue(int value) {
            this.defaultValue = () -> value;
            return this;
        }
        
        public Builder<S> min(int min) {
            this.min = min;
            return this;
        }
        
        public Builder<S> max(int max) {
            this.max = max;
            return this;
        }
        
        public RIntegerArgument<S> build() {
            return new RIntegerArgument<>(this);
        }
    }
}
