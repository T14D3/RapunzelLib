package de.t14d3.rapunzellib.commands.arguments;

import com.mojang.brigadier.arguments.StringArgumentType;
import de.t14d3.rapunzellib.commands.RCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * String argument type for command parsing.
 * <p>
 * Supports multiple string types (single word, quoted phrase, greedy phrase)
 * with optional arguments and tab completion suggestions.
 * </p>
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * // Required word argument
 * RStringArgument<RCommandSource> playerName = RStringArgument.word("player");
 * 
 * // Optional string with default
 * RStringArgument<RCommandSource> message = RStringArgument.optional("message", "Hello");
 * 
 * // Greedy (remaining text) argument
 * RStringArgument<RCommandSource> query = RStringArgument.greedy("query");
* 
 * // Word argument with suggestions
 * RStringArgument<RCommandSource> action = new RStringArgument<>("action")
 *     .type(StringType.SINGLE_WORD)
 *     .suggestions(Arrays.asList("create", "delete", "list"));
 * }</pre>
 * 
 * @param <S> the command source type
 */
public class RStringArgument<S extends RCommandSource> implements RArgument<S, String> {
    
    public enum StringType {
        SINGLE_WORD,
        QUOTABLE_PHRASE,
        GREEDY_PHRASE
    }
    
    private final String name;
    private final boolean optional;
    private final Supplier<String> defaultValue;
    private final StringArgumentType argumentType;
    private final StringType type;
    private List<String> suggestions;
    
    private RStringArgument(Builder<S> builder) {
        this.name = builder.name;
        this.optional = builder.optional;
        this.defaultValue = builder.defaultValue;
        this.type = builder.type;
        this.suggestions = builder.suggestions;
        
        switch (type) {
            case SINGLE_WORD:
                this.argumentType = StringArgumentType.word();
                break;
            case QUOTABLE_PHRASE:
                this.argumentType = StringArgumentType.string();
                break;
            case GREEDY_PHRASE:
                this.argumentType = StringArgumentType.greedyString();
                break;
            default:
                this.argumentType = StringArgumentType.word();
        }
    }
    
    public static <S extends RCommandSource> RStringArgument<S> word(@NotNull String name) {
        return new Builder<S>(name)
            .type(StringType.SINGLE_WORD)
            .optional(false)
            .build();
    }
    
    /**
     * Creates a required greedy phrase argument (captures remaining input).
     * 
     * @param name the argument name
     * @param <S> the command source type
     * @return a new greedy phrase argument
     */
    public static <S extends RCommandSource> RStringArgument<S> greedy(@NotNull String name) {
        return new Builder<S>(name)
            .type(StringType.GREEDY_PHRASE)
            .optional(false)
            .build();
    }
    
    public static <S extends RCommandSource> RStringArgument<S> optional(@NotNull String name, @Nullable String defaultValue) {
        return new Builder<S>(name)
            .type(StringType.SINGLE_WORD)
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
    public StringArgumentType getArgumentType() {
        return argumentType;
    }
    
    @Override
    public boolean isOptional() {
        return optional;
    }
    
    @Nullable
    @Override
    public Supplier<String> getDefaultValue() {
        return defaultValue;
    }
    
    @NotNull
    @Override
    public List<String> getSuggestions(@NotNull S source) {
        return suggestions != null ? suggestions : Collections.emptyList();
    }
    
    @Override
    public String parse(@NotNull String input) throws IllegalArgumentException {
        return input;
    }
    
    @NotNull
    public StringType getType() {
        return type;
    }
    
    public RStringArgument<S> suggestions(@NotNull List<String> suggestions) {
        this.suggestions = suggestions;
        return this;
    }
    
    /**
     * Builder for creating string arguments.
     */
    public static class Builder<S extends RCommandSource> {
        private final String name;
        private boolean optional = false;
        private Supplier<String> defaultValue = null;
        private StringType type = StringType.SINGLE_WORD;
        private List<String> suggestions = null;
        
        public Builder(@NotNull String name) {
            this.name = name;
        }
        
        public Builder<S> type(@NotNull StringType type) {
            this.type = type;
            return this;
        }
        
        public Builder<S> optional(boolean optional) {
            this.optional = optional;
            return this;
        }
        
        public Builder<S> defaultValue(@Nullable String value) {
            this.defaultValue = () -> value;
            return this;
        }
        
        public Builder<S> suggestions(@NotNull List<String> suggestions) {
            this.suggestions = suggestions;
            return this;
        }
        
        public Builder<S> suggestions(@NotNull String... suggestions) {
            this.suggestions = Arrays.asList(suggestions);
            return this;
        }
        
        public RStringArgument<S> build() {
            return new RStringArgument<>(this);
        }
    }
}
