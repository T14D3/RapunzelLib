package de.t14d3.rapunzellib.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.commands.RCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class RKeyArgument<S extends RCommandSource> implements RArgument<S, RKey> {
    private static final KeyArgumentType ARGUMENT_TYPE = new KeyArgumentType();

    private final String name;
    private final boolean optional;
    private final Supplier<RKey> defaultValue;
    private final List<String> suggestions;

    private RKeyArgument(
        @NotNull String name,
        boolean optional,
        @Nullable Supplier<RKey> defaultValue,
        @NotNull List<String> suggestions
    ) {
        this.name = name;
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.suggestions = List.copyOf(suggestions);
    }

    public static <S extends RCommandSource> @NotNull RKeyArgument<S> required(@NotNull String name) {
        return new RKeyArgument<>(name, false, null, List.of());
    }

    public static <S extends RCommandSource> @NotNull RKeyArgument<S> optional(
        @NotNull String name,
        @NotNull RKey defaultValue
    ) {
        return new RKeyArgument<>(name, true, () -> defaultValue, List.of());
    }

    public @NotNull RKeyArgument<S> suggestions(@NotNull Collection<String> values) {
        return new RKeyArgument<>(name, optional, defaultValue, List.copyOf(values));
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull ArgumentType<RKey> getArgumentType() {
        return ARGUMENT_TYPE;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    @Override
    public @Nullable Supplier<RKey> getDefaultValue() {
        return defaultValue;
    }

    @Override
    public @NotNull List<String> getSuggestions(@NotNull S source) {
        return suggestions;
    }

    @Override
    public RKey parse(@NotNull String input) throws IllegalArgumentException {
        return RKey.parse(input);
    }

    private static final class KeyArgumentType implements ArgumentType<RKey> {
        private static final DynamicCommandExceptionType INVALID_KEY =
            new DynamicCommandExceptionType(value -> () -> "Invalid key '" + value + "'");

        @Override
        public RKey parse(StringReader reader) throws CommandSyntaxException {
            String value = readKey(reader);
            try {
                return RKey.parse(value);
            } catch (IllegalArgumentException ex) {
                throw INVALID_KEY.create(value);
            }
        }

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
