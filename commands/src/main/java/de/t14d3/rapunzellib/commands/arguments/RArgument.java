package de.t14d3.rapunzellib.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.core.RCommandArguments;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface for typed command arguments in RapunzelLib.
 * <p>
 * This interface provides a way to define command arguments with
 * type safety, default values, and tab completion suggestions.
 * </p>
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Type-safe argument parsing</li>
 *   <li>Brigadier integration via {@link ArgumentType}</li>
 *   <li>Tab completion support via suggestion providers</li>
 *   <li>Optional arguments with default values</li>
 * </ul>
 * 
 * @param <S> the command source type
 * @param <T> the argument value type
 */
public interface RArgument<S extends RCommandSource, T> {
    
    @NotNull
    String getName();
    
    @NotNull
    ArgumentType<T> getArgumentType();
    
    boolean isOptional();
    
    Supplier<T> getDefaultValue();
    
    @NotNull
    List<String> getSuggestions(@NotNull S source);

    default @NotNull List<String> getSuggestions(
        @NotNull S source,
        @NotNull RCommandArguments<S> info
    ) {
        return getSuggestions(source);
    }

    default @NotNull CompletableFuture<Suggestions> listSuggestions(
        @NotNull S source,
        @NotNull SuggestionsBuilder builder
    ) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String suggestion : getSuggestions(source)) {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    }

    default @NotNull CompletableFuture<Suggestions> listSuggestions(
        @NotNull S source,
        @NotNull RCommandArguments<S> info
    ) {
        return info.suggestMatching(getSuggestions(source, info));
    }
    
    /**
     * Parses a string value into the argument type.
     * 
     * @param input the string input
     * @return the parsed value
     * @throws IllegalArgumentException if parsing fails
     */
    T parse(@NotNull String input) throws IllegalArgumentException;
    
    /**
     * Validates a parsed value.
     * 
     * @param value the value to validate
     * @return true if valid
     */
    default boolean isValid(@NotNull T value) {
        return true;
    }
}
