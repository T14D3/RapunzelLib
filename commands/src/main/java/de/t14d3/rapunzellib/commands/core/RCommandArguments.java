package de.t14d3.rapunzellib.commands.core;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryKey;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import de.t14d3.rapunzellib.registry.RRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Wrapper around Brigadier's {@link CommandContext} for convenient argument access.
 * <p>
 * This class provides a simpler API for accessing command arguments compared
 * to working directly with Brigadier's CommandContext. It handles type conversions
 * and optional arguments more conveniently.
 * </p>
 * <p>
 * All methods return {@link Optional} to gracefully handle missing arguments,
 * making it easier to write safe command code.
 * </p>
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * command.executes((source, args) -> {
 *     Optional<String> name = args.getString("name");
 *     Optional<Integer> amount = args.getInteger("amount");
 *
 *     if (name.isEmpty() || amount.isEmpty()) {
 *         throw new CommandException("Missing required arguments");
 *     }
 *
 *     source.sendMessage("Giving " + amount.get() + " of " + name.get());
 *     return CommandResult.SUCCESS;
 * });
 * }</pre>
 *
 * @param <S> the command source type
 * @see RCommandExecutor
 */
public final class RCommandArguments<S> {

    private final CommandContext<?> context;
    private final S source;
    private final RCommandNode<?> node;
    private final SuggestionsBuilder suggestionsBuilder;

    public RCommandArguments(@NotNull CommandContext<S> context) {
        this(context.getSource(), context, null, null);
    }

    public RCommandArguments(@NotNull S source, @NotNull CommandContext<?> context) {
        this(source, context, null, null);
    }

    public RCommandArguments(
        @NotNull S source,
        @NotNull CommandContext<?> context,
        @Nullable RCommandNode<?> node
    ) {
        this(source, context, node, null);
    }

    /**
     * Creates a CommandArguments wrapper with source, context, node, and suggestions builder.
     *
     * @param source             the command source
     * @param context            the underlying Brigadier command context
     * @param node               the current command node, may be null
     * @param suggestionsBuilder the Brigadier suggestions builder, may be null
     */
    public RCommandArguments(
        @NotNull S source,
        @NotNull CommandContext<?> context,
        @Nullable RCommandNode<?> node,
        @Nullable SuggestionsBuilder suggestionsBuilder
    ) {
        this.source = Objects.requireNonNull(source, "source");
        this.context = Objects.requireNonNull(context, "context");
        this.node = node;
        this.suggestionsBuilder = suggestionsBuilder;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public CommandContext<S> getContext() {
        return (CommandContext<S>) context;
    }

    @NotNull
    public Optional<String> getString(@NotNull String name) {
        try {
            String value = context.getArgument(name, String.class);
            return Optional.ofNullable(value);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @NotNull
    public Optional<Integer> getInteger(@NotNull String name) {
        try {
            Integer value = context.getArgument(name, int.class);
            return Optional.ofNullable(value);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @NotNull
    public Optional<Long> getLong(@NotNull String name) {
        try {
            Long value = context.getArgument(name, long.class);
            return Optional.ofNullable(value);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @NotNull
    public Optional<Double> getDouble(@NotNull String name) {
        try {
            Double value = context.getArgument(name, double.class);
            return Optional.ofNullable(value);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @NotNull
    public Optional<Float> getFloat(@NotNull String name) {
        try {
            Float value = context.getArgument(name, float.class);
            return Optional.ofNullable(value);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @NotNull
    public Optional<Boolean> getBoolean(@NotNull String name) {
        try {
            Boolean value = context.getArgument(name, boolean.class);
            return Optional.ofNullable(value);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @NotNull
    public Optional<RKey> getKey(@NotNull String name) {
        Optional<RKey> typedKey = get(name, RKey.class);
        if (typedKey.isPresent()) {
            return typedKey;
        }
        return getString(name).flatMap(RKey::tryParse);
    }

    @NotNull
    public <T> Optional<RRegistryRef<T>> getRegistryRef(@NotNull String name, @NotNull RRegistryKey<T> registryKey) {
        Objects.requireNonNull(registryKey, "registryKey");
        Optional<RRegistryRef<T>> typedRef = getTypedRegistryRef(name, registryKey);
        if (typedRef.isPresent()) {
            return typedRef;
        }
        return getKey(name).map(registryKey::ref);
    }

    @NotNull
    public Optional<RRegistryRef<REntityType>> getEntityTypeRef(@NotNull String name) {
        return getRegistryRef(name, RRegistries.ENTITY_TYPES);
    }

    @NotNull
    public Optional<REntityType> getEntityType(@NotNull String name) {
        return resolveRegistryRef(getEntityTypeRef(name));
    }

    @NotNull
    public Optional<RRegistryRef<RBlockType>> getBlockTypeRef(@NotNull String name) {
        return getRegistryRef(name, RRegistries.BLOCK_TYPES);
    }

    @NotNull
    public Optional<RBlockType> getBlockType(@NotNull String name) {
        return resolveRegistryRef(getBlockTypeRef(name));
    }

    @NotNull
    public Optional<RRegistryRef<RItemType>> getItemTypeRef(@NotNull String name) {
        return getRegistryRef(name, RRegistries.ITEM_TYPES);
    }

    @NotNull
    public Optional<RItemType> getItemType(@NotNull String name) {
        return resolveRegistryRef(getItemTypeRef(name));
    }

    @SuppressWarnings("unchecked")
    private <T> @NotNull Optional<RRegistryRef<T>> getTypedRegistryRef(
        @NotNull String name,
        @NotNull RRegistryKey<T> registryKey
    ) {
        Optional<RRegistryRef> rawRef = get(name, RRegistryRef.class);
        if (rawRef.isEmpty()) {
            return Optional.empty();
        }
        RRegistryRef<?> value = rawRef.get();
        if (!value.registryKey().equals(registryKey)) {
            return Optional.empty();
        }
        return Optional.of((RRegistryRef<T>) value);
    }

    private <T> @NotNull Optional<T> resolveRegistryRef(@NotNull Optional<RRegistryRef<T>> ref) {
        return ref.flatMap(value -> Rapunzel.findContext()
            .map(context -> value.find(context.registries()))
            .orElse(Optional.empty()));
    }

    @NotNull
    public <T> Optional<T> get(@NotNull String name, @NotNull Class<T> type) {
        try {
            T value = context.getArgument(name, type);
            return Optional.ofNullable(value);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @NotNull
    public S getSource() {
        return source;
    }

    @NotNull
    public S sender() {
        return source;
    }

    @NotNull
    public Optional<Object> getNativeSource() {
        if (source instanceof RNativeHandle<?> nativeHandle) {
            return Optional.of(nativeHandle.handle());
        }
        return Optional.empty();
    }

    @Nullable
    public RCommandNode<?> getNode() {
        return node;
    }

    @NotNull
    public Optional<RCommandNode<?>> currentNode() {
        return Optional.ofNullable(node);
    }

    /**
     * Checks if an argument exists.
     *
     * @param name the argument name
     * @return true if the argument exists
     */
    public boolean has(@NotNull String name) {
        try {
            context.getArgument(name, Object.class);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @NotNull
    public <T> T getOrDefault(@NotNull String name, @NotNull T defaultValue) {
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) defaultValue.getClass();
        return get(name, type).orElse(defaultValue);
    }

    @NotNull
    public String getInput() {
        return context.getInput();
    }

    /**
     * Gets the current remaining input text, considering suggestion context.
     *
     * @return the current input
     */
    @NotNull
    public String getCurrentInput() {
        if (suggestionsBuilder != null) {
            return suggestionsBuilder.getRemaining();
        }
        return currentRange()
            .map(range -> range.get(context.getInput()))
            .filter(value -> !value.isEmpty())
            .orElseGet(context::getInput);
    }

    /**
     * Checks if this context is collecting suggestions (tab completion).
     *
     * @return true if in suggestion context
     */
    public boolean isSuggestionContext() {
        return suggestionsBuilder != null;
    }

    @NotNull
    public Optional<SuggestionsBuilder> getSuggestionsBuilder() {
        return Optional.ofNullable(suggestionsBuilder);
    }

    @Contract("-> fail")
    private static SuggestionsBuilder missingSuggestionsBuilder() {
        throw new IllegalStateException("This command context is not currently collecting suggestions");
    }

    /**
     * Gets the required suggestions builder, throwing if not available.
     *
     * @return the suggestions builder
     * @throws IllegalStateException if not in a suggestion context
     */
    @NotNull
    public SuggestionsBuilder requireSuggestionsBuilder() {
        return suggestionsBuilder != null ? suggestionsBuilder : missingSuggestionsBuilder();
    }

    /**
     * Suggests all given strings.
     *
     * @param suggestions the suggestions to add
     * @return a future containing the suggestions
     */
    @NotNull
    public CompletableFuture<Suggestions> suggestAll(@NotNull Collection<String> suggestions) {
        Objects.requireNonNull(suggestions, "suggestions");
        SuggestionsBuilder builder = requireSuggestionsBuilder();
        for (String suggestion : suggestions) {
            builder.suggest(suggestion);
        }
        return builder.buildFuture();
    }

    /**
     * Suggests strings that match the current remaining input.
     *
     * @param suggestions the suggestions to filter
     * @return a future containing the matching suggestions
     */
    @NotNull
    public CompletableFuture<Suggestions> suggestMatching(@NotNull Collection<String> suggestions) {
        Objects.requireNonNull(suggestions, "suggestions");
        SuggestionsBuilder builder = requireSuggestionsBuilder();
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    }

    /**
     * Builds suggestions from the current builder state.
     *
     * @return a future containing the suggestions
     */
    @NotNull
    public CompletableFuture<Suggestions> buildSuggestions() {
        return requireSuggestionsBuilder().buildFuture();
    }

    @NotNull
    public Map<String, Object> getParsedArguments() {
        Map<String, Object> parsed = new LinkedHashMap<>();
        for (ParsedCommandNode<?> parsedNode : context.getNodes()) {
            String name = parsedNode.getNode().getName();
            try {
                parsed.put(name, context.getArgument(name, Object.class));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Collections.unmodifiableMap(parsed);
    }

    /**
     * Gets a map of all parsed argument names to their raw string values.
     *
     * @return an unmodifiable map of argument names to raw strings
     */
    @NotNull
    public Map<String, String> getRawArguments() {
        Map<String, String> parsed = new LinkedHashMap<>();
        String input = context.getInput();
        for (ParsedCommandNode<?> parsedNode : context.getNodes()) {
            String name = parsedNode.getNode().getName();
            try {
                context.getArgument(name, Object.class);
                parsed.put(name, parsedNode.getRange().get(input));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Collections.unmodifiableMap(parsed);
    }

    /**
     * Gets the raw string values of arguments parsed before the current node.
     *
     * @return a list of previous argument values
     */
    @NotNull
    public List<String> getPreviousArguments() {
        List<Map.Entry<String, String>> entries = new java.util.ArrayList<>(getRawArguments().entrySet());
        if (isSuggestionContext() && node != null) {
            for (int index = entries.size() - 1; index >= 0; index--) {
                if (entries.get(index).getKey().equals(node.getName())) {
                    entries.remove(index);
                    break;
                }
            }
        }
        return entries.stream().map(Map.Entry::getValue).toList();
    }

    @NotNull
    public Optional<String> getRawArgument(@NotNull String name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(getRawArguments().get(name));
    }

    @NotNull
    public List<String> getParsedNodeNames() {
        return context.getNodes().stream()
            .map(parsedNode -> parsedNode.getNode().getName())
            .toList();
    }

    private @NotNull Optional<com.mojang.brigadier.context.StringRange> currentRange() {
        List<? extends ParsedCommandNode<?>> nodes = context.getNodes();
        if (nodes.isEmpty()) {
            return Optional.empty();
        }
        if (node == null) {
            return Optional.of(nodes.get(nodes.size() - 1).getRange());
        }
        for (int index = nodes.size() - 1; index >= 0; index--) {
            ParsedCommandNode<?> parsedNode = nodes.get(index);
            if (parsedNode.getNode().getName().equals(node.getName())) {
                return Optional.of(parsedNode.getRange());
            }
        }
        return Optional.of(nodes.get(nodes.size() - 1).getRange());
    }
}
