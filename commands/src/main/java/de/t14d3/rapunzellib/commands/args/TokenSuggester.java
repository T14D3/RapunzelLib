package de.t14d3.rapunzellib.commands.args;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.t14d3.rapunzellib.commands.RCommandSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface TokenSuggester {
    
    CompletableFuture<Suggestions> suggest(
        RCommandSource source,
        List<String> tokens,
        String partial,
        SuggestionsBuilder builder
    );
}
