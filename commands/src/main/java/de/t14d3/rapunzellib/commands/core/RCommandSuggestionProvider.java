package de.t14d3.rapunzellib.commands.core;

import com.mojang.brigadier.suggestion.Suggestions;
import de.t14d3.rapunzellib.commands.RCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface RCommandSuggestionProvider<S extends RCommandSource> {
    @NotNull CompletableFuture<Suggestions> suggest(
        @NotNull S source,
        @NotNull RCommandArguments<S> info
    );
}
