package de.t14d3.rapunzellib.commands.core;

import com.mojang.brigadier.suggestion.Suggestions;
import de.t14d3.rapunzellib.commands.RCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Provides tab-completion suggestions for a command node.
 * <p>
 * Implementations generate {@link Suggestions} based on the command source
 * and the current argument context. This is the functional interface for
 * command suggestion logic.
 * </p>
 *
 * @param <S> the command source type
 */
@FunctionalInterface
public interface RCommandSuggestionProvider<S extends RCommandSource> {
    /**
     * Generates suggestions for the current command argument.
     *
     * @param source the command source
     * @param info   the current argument context
     * @return a future containing the suggestions
     */
    @NotNull CompletableFuture<Suggestions> suggest(
        @NotNull S source,
        @NotNull RCommandArguments<S> info
    );
}
