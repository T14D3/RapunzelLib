package de.t14d3.rapunzellib.commands.args;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.t14d3.rapunzellib.commands.RCommandSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface TokenSuggester {
    /**
     * @param source the command source (must be the dispatcher's source type)
     * @param tokens fully parsed tokens before the current token
     * @param partial the current (possibly empty) token to complete
     * @param builder a suggestions builder offset to the start of {@code partial}
     */
    CompletableFuture<Suggestions> suggest(
        RCommandSource source,
        List<String> tokens,
        String partial,
        SuggestionsBuilder builder
    );
}

