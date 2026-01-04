package de.t14d3.rapunzellib.commands.args;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.t14d3.rapunzellib.commands.RCommandSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A delimiter-based list argument inspired by CommandAPI's list argument.
 *
 * <p>This argument type parses as a {@link String} (either {@code string()} or {@code greedyString()}),
 * but provides {@link #getList(CommandContext, String)} to convert/validate the typed values
 * using the provided supplier and mapper.</p>
 *
 * <p>Why not parse to {@code List<T>} directly? Brigadier's {@link ArgumentType#parse(StringReader)}
 * has no access to the sender/context, so validation against a dynamic allowed-values supplier
 * must happen in {@link #getList(CommandContext, String)}.</p>
 */
public final class ListArgumentType<T> implements ArgumentType<String> {
    private final ArgumentType<String> delegate;
    private final String delimiter;
    private final boolean allowDuplicates;
    private final boolean text;
    private final Function<Object, RCommandSource> sourceAdapter;
    private final BiFunction<RCommandSource, CommandContext<?>, Collection<T>> supplier;
    private final Function<T, Suggestion> mapper;
    private final SimpleCommandExceptionType notAllowedException;
    private final SimpleCommandExceptionType duplicateException;

    private ListArgumentType(Builder<T> b) {
        this.delegate = b.text ? StringArgumentType.string() : StringArgumentType.greedyString();
        this.delimiter = Objects.requireNonNull(b.delimiter, "delimiter");
        this.allowDuplicates = b.allowDuplicates;
        this.text = b.text;
        this.sourceAdapter = b.sourceAdapter;
        this.supplier = Objects.requireNonNull(b.supplier, "supplier");
        this.mapper = Objects.requireNonNull(b.mapper, "mapper");
        this.notAllowedException = new SimpleCommandExceptionType(new LiteralMessage(b.notAllowedMessage));
        this.duplicateException = new SimpleCommandExceptionType(new LiteralMessage(b.duplicateMessage));
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Converts and validates the parsed string argument into a typed list.
     *
     * @throws CommandSyntaxException if an element is not allowed or duplicates are not allowed
     */
    public <S> List<T> getList(CommandContext<S> ctx, String key) throws CommandSyntaxException {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(key, "key");

        String argument = ctx.getArgument(key, String.class);
        RCommandSource rSource = adaptSource(ctx);

        Map<String, T> values = new HashMap<>();
        for (T object : supplier.apply(rSource, ctx)) {
            Suggestion suggestion = mapper.apply(object);
            values.put(suggestion.suggestion(), object);
        }

        List<T> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        String[] parts = argument.split(Pattern.quote(delimiter));
        StringReader reader = new StringReader(argument);
        for (String part : parts) {
            if (!values.containsKey(part)) {
                throw notAllowedException.createWithContext(reader);
            }
            if (!allowDuplicates && !seen.add(part)) {
                throw duplicateException.createWithContext(reader);
            }

            out.add(values.get(part));
            reader.setCursor(reader.getCursor() + part.length() + delimiter.length());
        }

        return List.copyOf(out);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return delegate.parse(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String currentArg = builder.getRemaining();
        if (text && currentArg.startsWith("\"")) {
            currentArg = currentArg.substring(1);
            builder = builder.createOffset(builder.getStart() + 1);
        }

        RCommandSource rSource = adaptSource(context);
        Set<Suggestion> suggestions = new HashSet<>();
        for (T object : supplier.apply(rSource, context)) {
            suggestions.add(mapper.apply(object));
        }

        String[] splitArguments = currentArg.split(Pattern.quote(delimiter));
        String lastArgument = splitArguments[splitArguments.length - 1];

        if (!currentArg.endsWith(delimiter) && suggestions.stream().map(Suggestion::suggestion).anyMatch(lastArgument::equals)) {
            suggestions.add(Suggestion.none(lastArgument + delimiter));
        }

        if (!allowDuplicates) {
            for (String str : splitArguments) {
                suggestions.removeIf(s -> s.suggestion().equals(str));
            }
        }

        if (currentArg.contains(delimiter)) {
            builder = builder.createOffset(builder.getStart() + currentArg.lastIndexOf(delimiter) + delimiter.length());
        }

        String remaining = builder.getRemaining();
        for (Suggestion s : suggestions) {
            String suggestion = s.suggestion();
            if (!suggestion.startsWith(remaining)) continue;
            if (s.tooltip() == null) builder.suggest(suggestion);
            else builder.suggest(suggestion, s.tooltip());
        }

        return builder.buildFuture();
    }

    private RCommandSource adaptSource(CommandContext<?> context) {
        if (sourceAdapter == null) return null;
        Object source = context.getSource();
        try {
            return (source != null) ? sourceAdapter.apply(source) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static final class Builder<T> {
        private String delimiter = " ";
        private boolean allowDuplicates = true;
        private boolean text = false;
        private Function<Object, RCommandSource> sourceAdapter = null;
        private BiFunction<RCommandSource, CommandContext<?>, Collection<T>> supplier;
        private Function<T, Suggestion> mapper = t -> Suggestion.none(String.valueOf(t));
        private String notAllowedMessage = "Item is not allowed in list";
        private String duplicateMessage = "Duplicate arguments are not allowed";

        private Builder() {
        }

        public Builder<T> delimiter(String delimiter) {
            this.delimiter = Objects.requireNonNull(delimiter, "delimiter");
            return this;
        }

        public Builder<T> allowDuplicates(boolean allowDuplicates) {
            this.allowDuplicates = allowDuplicates;
            return this;
        }

        /**
         * When enabled, parses as {@link StringArgumentType#string()} and applies the
         * leading-quote suggestion offset used by CommandAPI's text argument mode.
         */
        public Builder<T> text(boolean text) {
            this.text = text;
            return this;
        }

        public Builder<T> sourceAdapter(Function<Object, RCommandSource> sourceAdapter) {
            this.sourceAdapter = Objects.requireNonNull(sourceAdapter, "sourceAdapter");
            return this;
        }

        public Builder<T> values(BiFunction<RCommandSource, CommandContext<?>, Collection<T>> supplier) {
            this.supplier = Objects.requireNonNull(supplier, "supplier");
            return this;
        }

        public Builder<T> values(Collection<T> values) {
            Objects.requireNonNull(values, "values");
            this.supplier = (src, ctx) -> values;
            return this;
        }

        public Builder<T> mapper(Function<T, Suggestion> mapper) {
            this.mapper = Objects.requireNonNull(mapper, "mapper");
            return this;
        }

        public Builder<T> notAllowedMessage(String message) {
            this.notAllowedMessage = Objects.requireNonNull(message, "message");
            return this;
        }

        public Builder<T> duplicateMessage(String message) {
            this.duplicateMessage = Objects.requireNonNull(message, "message");
            return this;
        }

        public ListArgumentType<T> build() {
            if (supplier == null) throw new IllegalStateException("values supplier is required");
            if (delimiter.isEmpty()) throw new IllegalStateException("delimiter cannot be empty");
            return new ListArgumentType<>(this);
        }
    }

    public record Suggestion(String suggestion, com.mojang.brigadier.Message tooltip) {
        public Suggestion {
            if (suggestion == null || suggestion.isBlank()) {
                throw new IllegalArgumentException("suggestion cannot be null/blank");
            }
        }

        public static Suggestion none(String suggestion) {
            return new Suggestion(suggestion, null);
        }
    }
}

