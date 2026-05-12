package de.t14d3.rapunzellib.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistries;
import de.t14d3.rapunzellib.registry.RRegistry;
import de.t14d3.rapunzellib.registry.RRegistryKey;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Argument type for {@link RRegistryRef} values, allowing players to specify
 * registry entries (items, blocks, entity types, etc.) by their namespaced key.
 * <p>
 * Provides convenience factory methods for common registries like item types,
 * block types, and entity types. Tab completion suggests all keys from the
 * associated registry.
 * </p>
 *
 * @param <S> the command source type
 * @param <T> the registry entry type
 */
public final class RRegistryRefArgument<S extends RCommandSource, T> implements RArgument<S, RRegistryRef<T>> {
    private final String name;
    /**
     * The registry key identifying which registry this argument references.
     */
    private final RRegistryKey<T> registryKey;
    private final boolean optional;
    private final Supplier<RRegistryRef<T>> defaultValue;
    private final RegistryRefArgumentType<T> argumentType;

    private RRegistryRefArgument(
        @NotNull String name,
        @NotNull RRegistryKey<T> registryKey,
        boolean optional,
        @Nullable Supplier<RRegistryRef<T>> defaultValue
    ) {
        this.name = name;
        this.registryKey = registryKey;
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.argumentType = new RegistryRefArgumentType<>(registryKey);
    }

    /**
     * Creates a required registry reference argument.
     *
     * @param name        the argument name
     * @param registryKey the registry key
     * @param <S>         the command source type
     * @param <T>         the registry entry type
     * @return the registry ref argument
     */
    public static <S extends RCommandSource, T> @NotNull RRegistryRefArgument<S, T> registryRef(
        @NotNull String name,
        @NotNull RRegistryKey<T> registryKey
    ) {
        return new RRegistryRefArgument<>(name, registryKey, false, null);
    }

    /**
     * Creates a registry ref argument for entity types.
     *
     * @param name the argument name
     * @param <S>  the command source type
     * @return the entity type ref argument
     */
    public static <S extends RCommandSource> @NotNull RRegistryRefArgument<S, REntityType> entityType(@NotNull String name) {
        return registryRef(name, RRegistries.ENTITY_TYPES);
    }

    /**
     * Creates a registry ref argument for block types.
     *
     * @param name the argument name
     * @param <S>  the command source type
     * @return the block type ref argument
     */
    public static <S extends RCommandSource> @NotNull RRegistryRefArgument<S, RBlockType> blockType(@NotNull String name) {
        return registryRef(name, RRegistries.BLOCK_TYPES);
    }

    /**
     * Creates a registry ref argument for item types.
     *
     * @param name the argument name
     * @param <S>  the command source type
     * @return the item type ref argument
     */
    public static <S extends RCommandSource> @NotNull RRegistryRefArgument<S, RItemType> itemType(@NotNull String name) {
        return registryRef(name, RRegistries.ITEM_TYPES);
    }

    /**
     * Creates a new instance with this argument as optional with the given default value.
     *
     * @param value the default registry ref value
     * @return a new optional registry ref argument
     */
    public @NotNull RRegistryRefArgument<S, T> optional(@NotNull RRegistryRef<T> value) {
        return new RRegistryRefArgument<>(name, registryKey, true, () -> value);
    }

    /**
     * Gets the registry key for this argument.
     *
     * @return the registry key
     */
    public @NotNull RRegistryKey<T> registryKey() {
        return registryKey;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull ArgumentType<RRegistryRef<T>> getArgumentType() {
        return argumentType;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    @Override
    public @Nullable Supplier<RRegistryRef<T>> getDefaultValue() {
        return defaultValue;
    }

    @Override
    public @NotNull List<String> getSuggestions(@NotNull S source) {
        return Rapunzel.findContext()
            .flatMap(context -> context.registries().findRegistry(registryKey))
            .map(RRegistry::keys)
            .orElseGet(List::of)
            .stream()
            .map(RKey::asString)
            .toList();
    }

    /**
     * Parses an input string into an {@link RRegistryRef}.
     *
     * @param input the string input
     * @return the parsed registry ref
     * @throws IllegalArgumentException if the input is not a valid key
     */
    @Override
    public RRegistryRef<T> parse(@NotNull String input) throws IllegalArgumentException {
        return registryKey.ref(RKey.parse(input));
    }

    /**
     * Brigadier {@link ArgumentType} that parses {@link RRegistryRef} values from command input.
     *
     * @param <T> the registry entry type
     */
    private static final class RegistryRefArgumentType<T> implements ArgumentType<RRegistryRef<T>> {
        private static final DynamicCommandExceptionType INVALID_KEY =
            new DynamicCommandExceptionType(value -> () -> "Invalid key '" + value + "'");

        /**
         * The registry key for this argument type.
         */
        private final RRegistryKey<T> registryKey;

        /**
         * Creates a new registry ref argument type.
         *
         * @param registryKey the registry key
         */
        private RegistryRefArgumentType(@NotNull RRegistryKey<T> registryKey) {
            this.registryKey = registryKey;
        }

        /**
         * Parses a registry ref value from the reader.
         *
         * @param reader the Brigadier string reader
         * @return the parsed registry ref
         * @throws CommandSyntaxException if the key is invalid
         */
        @Override
        public RRegistryRef<T> parse(StringReader reader) throws CommandSyntaxException {
            String value = readKey(reader);
            try {
                return registryKey.ref(RKey.parse(value));
            } catch (IllegalArgumentException ex) {
                throw INVALID_KEY.create(value);
            }
        }

        /**
         * Reads a key string from the reader, handling quoted strings.
         *
         * @param reader the Brigadier string reader
         * @return the key string
         * @throws CommandSyntaxException if reading fails
         */
        private static @NotNull String readKey(@NotNull StringReader reader) throws CommandSyntaxException {
            if (reader.canRead() && reader.peek() == '"') {
                return reader.readQuotedString();
            }

            int start = reader.getCursor();
            while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
                reader.skip();
            }
            return reader.getString().substring(start, reader.getCursor());
        }

        @Override
        public @NotNull Collection<String> getExamples() {
            return Collections.singleton("minecraft:stone");
        }
    }
}
