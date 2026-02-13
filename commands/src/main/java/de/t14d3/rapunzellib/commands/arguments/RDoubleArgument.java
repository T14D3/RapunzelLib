package de.t14d3.rapunzellib.commands.arguments;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import de.t14d3.rapunzellib.commands.RCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Double argument type for command parsing.
 * <p>
 * Supports optional arguments with default values and range validation.
 * </p>
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * // Required double argument
 * RDoubleArgument<RCommandSource> speed = RDoubleArgument.required("speed");
 * 
 * // Optional double with default value
 * RDoubleArgument<RCommandSource> scale = RDoubleArgument.optional("scale", 1.0);
 * 
 * // Double with min/max validation
 * RDoubleArgument<RCommandSource> percentage = new RDoubleArgument<>("percentage")
 *     .min(0.0).max(100.0);
 * }</pre>
 * 
 * @param <S> the command source type
 */
public class RDoubleArgument<S extends RCommandSource> implements RArgument<S, Double> {
    
    private final String name;
    private final boolean optional;
    private final Supplier<Double> defaultValue;
    private final DoubleArgumentType argumentType;
    private final Double min;
    private final Double max;
    
    private RDoubleArgument(Builder<S> builder) {
        this.name = builder.name;
        this.optional = builder.optional;
        this.defaultValue = builder.defaultValue;
        this.min = builder.min;
        this.max = builder.max;
        
        if (min != null && max != null) {
            this.argumentType = DoubleArgumentType.doubleArg(min, max);
        } else if (min != null) {
            this.argumentType = DoubleArgumentType.doubleArg(min);
        } else {
            this.argumentType = DoubleArgumentType.doubleArg();
        }
    }
    
    /**
     * Creates a required double argument.
     * 
     * @param name the argument name
     * @param <S> the command source type
     * @return a new double argument
     */
    public static <S extends RCommandSource> RDoubleArgument<S> required(@NotNull String name) {
        return new Builder<S>(name)
            .optional(false)
            .build();
    }
    
    /**
     * Creates an optional double argument with a default value.
     * 
     * @param name the argument name
     * @param defaultValue the default value
     * @param <S> the command source type
     * @return a new double argument
     */
    public static <S extends RCommandSource> RDoubleArgument<S> optional(@NotNull String name, double defaultValue) {
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
    public DoubleArgumentType getArgumentType() {
        return argumentType;
    }
    
    @Override
    public boolean isOptional() {
        return optional;
    }
    
    @Nullable
    @Override
    public Supplier<Double> getDefaultValue() {
        return defaultValue;
    }
    
    @NotNull
    @Override
    public List<String> getSuggestions(@NotNull S source) {
        return Collections.emptyList();
    }
    
    @Override
    public Double parse(@NotNull String input) throws IllegalArgumentException {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + input + "' is not a valid double");
        }
    }
    
    @Override
    public boolean isValid(@NotNull Double value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }
    
    /**
     * Gets the minimum value for this argument.
     * 
     * @return the minimum value, or null if not set
     */
    @Nullable
    public Double getMin() {
        return min;
    }
    
    /**
     * Gets the maximum value for this argument.
     * 
     * @return the maximum value, or null if not set
     */
    @Nullable
    public Double getMax() {
        return max;
    }
    
    /**
     * Builder for creating double arguments.
     */
    public static class Builder<S extends RCommandSource> {
        private final String name;
        private boolean optional = false;
        private Supplier<Double> defaultValue = null;
        private Double min = null;
        private Double max = null;
        
        public Builder(@NotNull String name) {
            this.name = name;
        }
        
        public Builder<S> optional(boolean optional) {
            this.optional = optional;
            return this;
        }
        
        public Builder<S> defaultValue(double value) {
            this.defaultValue = () -> value;
            return this;
        }
        
        public Builder<S> min(double min) {
            this.min = min;
            return this;
        }
        
        public Builder<S> max(double max) {
            this.max = max;
            return this;
        }
        
        public RDoubleArgument<S> build() {
            return new RDoubleArgument<>(this);
        }
    }
}
