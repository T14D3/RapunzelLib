package de.t14d3.rapunzellib.commands.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.t14d3.rapunzellib.commands.RCommandSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Reads a whitespace-separated list of Brigadier strings until end-of-input.
 * <p>
 * This is the Brigadier equivalent to CommandAPI's "list argument" pattern for
 * "flag set" commands.
 */
public final class StringListArgumentType implements ArgumentType<List<String>> {
    private static final Collection<String> EXAMPLES = List.of("foo bar", "\"hello world\" baz", "!dirt *");

    private final TokenSuggester suggester;

    private StringListArgumentType(TokenSuggester suggester) {
        this.suggester = suggester;
    }

    public static StringListArgumentType stringList() {
        return new StringListArgumentType(null);
    }

    public static StringListArgumentType stringList(TokenSuggester suggester) {
        return new StringListArgumentType(Objects.requireNonNull(suggester, "suggester"));
    }

    @Override
    public List<String> parse(StringReader reader) throws CommandSyntaxException {
        List<String> out = new ArrayList<>();
        while (true) {
            reader.skipWhitespace();
            if (!reader.canRead()) break;
            out.add(reader.readString());
        }
        return List.copyOf(out);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (suggester == null) return ArgumentType.super.listSuggestions(context, builder);
        Object source = context.getSource();
        if (!(source instanceof RCommandSource rSource)) {
            return ArgumentType.super.listSuggestions(context, builder);
        }

        String remaining = builder.getRemaining();
        Tokenizer.Tokenization tokenization = Tokenizer.tokenizeForSuggestions(remaining);
        SuggestionsBuilder tokenBuilder = builder.createOffset(builder.getStart() + tokenization.partialStart());
        return suggester.suggest(rSource, tokenization.tokens(), tokenization.partial(), tokenBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}

