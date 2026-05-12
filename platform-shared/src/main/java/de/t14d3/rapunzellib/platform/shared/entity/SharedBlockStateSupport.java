package de.t14d3.rapunzellib.platform.shared.entity;

import com.mojang.brigadier.StringReader;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryHandles;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
// #if VERSION >= 1.21.11
import net.minecraft.resources.Identifier;
// #else
import net.minecraft.resources.ResourceLocation;
// #endif
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility for parsing {@link BlockState} from string representations.
 * <p>
 * Supports standard Minecraft block state strings (e.g. {@code minecraft:oak_log[axis=y]})
 * as well as fallback parsing for expressions that the standard parser rejects.
 * </p>
 */
final class SharedBlockStateSupport {
    private SharedBlockStateSupport() {
    }

    /**
     * Parses a string into a {@link BlockState}, trying the standard Minecraft parser first,
     * then falling back to a manual property-based parser.
     *
     * @param value the block state string to parse
     * @return an Optional containing the parsed BlockState, or empty if parsing failed
     */
    static @NotNull Optional<BlockState> parse(@NotNull String value) {
        Objects.requireNonNull(value, "value");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        try {
            BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, new StringReader(trimmed), false);
            return Optional.of(result.blockState());
        } catch (Exception ignored) {
        }

        ParsedState parsed = parseState(trimmed).orElse(null);
        if (parsed == null) {
            return Optional.empty();
        }

        Block block = RRegistryHandles.find(RBlockType.ref(parsed.key.toString()), Block.class)
            .orElseGet(() -> BuiltInRegistries.BLOCK.getValue(parsed.key));
        if (block == null) {
            return Optional.empty();
        }

        BlockState state = block.defaultBlockState();
        for (String assignment : parsed.assignments) {
            state = applyProperty(state, assignment).orElse(null);
            if (state == null) {
                return Optional.empty();
            }
        }
        return Optional.of(state);
    }

    /**
     * Applies a single property assignment (e.g. {@code axis=y}) to the given block state.
     *
     * @param state      the current block state
     * @param assignment the property assignment string
     * @return an Optional containing the updated BlockState, or empty if the property is invalid
     */
    private static @NotNull Optional<BlockState> applyProperty(@NotNull BlockState state, @NotNull String assignment) {
        int separator = assignment.indexOf('=');
        if (separator <= 0 || separator == assignment.length() - 1) {
            return Optional.empty();
        }

        String name = assignment.substring(0, separator).trim();
        String value = assignment.substring(separator + 1).trim();
        if (name.isEmpty() || value.isEmpty()) {
            return Optional.empty();
        }

        for (Property<?> property : state.getProperties()) {
            if (!property.getName().equals(name)) {
                continue;
            }
            return setPropertyValue(state, property, value);
        }
        return Optional.empty();
    }

    /**
     * Sets a typed property value on a block state.
     *
     * @param <T>      the property's value type
     * @param state    the current block state
     * @param property the property to set
     * @param value    the string representation of the property value
     * @return an Optional containing the updated BlockState, or empty if the value is invalid
     */
    private static <T extends Comparable<T>> @NotNull Optional<BlockState> setPropertyValue(
        @NotNull BlockState state,
        @NotNull Property<T> property,
        @NotNull String value
    ) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed));
    }

    /**
     * Parses a block state string into its block identifier and property assignments.
     *
     * @param value the raw block state string
     * @return an Optional containing the parsed state components
     */
    private static @NotNull Optional<ParsedState> parseState(@NotNull String value) {
        int bracketIndex = value.indexOf('[');
        String id = (bracketIndex >= 0 ? value.substring(0, bracketIndex) : value).trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty()) {
            return Optional.empty();
        }

        // #if VERSION >= 1.21.11
        Identifier key = id.contains(":") ? Identifier.tryParse(id) : Identifier.withDefaultNamespace(id);
        // #else
        ResourceLocation key = id.contains(":") ? ResourceLocation.tryParse(id) : ResourceLocation.withDefaultNamespace(id);
        // #endif
        if (key == null) {
            return Optional.empty();
        }

        if (bracketIndex < 0) {
            return Optional.of(new ParsedState(key, new String[0]));
        }

        int endBracket = value.lastIndexOf(']');
        if (endBracket <= bracketIndex) {
            return Optional.empty();
        }

        String body = value.substring(bracketIndex + 1, endBracket).trim();
        if (body.isEmpty()) {
            return Optional.of(new ParsedState(key, new String[0]));
        }

        String[] assignments = body.split(",");
        for (int index = 0; index < assignments.length; index++) {
            assignments[index] = assignments[index].trim();
            if (assignments[index].isEmpty()) {
                return Optional.empty();
            }
        }
        return Optional.of(new ParsedState(key, assignments));
    }

    // #if VERSION >= 1.21.11
    private record ParsedState(@NotNull Identifier key, @NotNull String[] assignments) {
        private ParsedState {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(assignments, "assignments");
        }
    }
    // #else
    private record ParsedState(@NotNull ResourceLocation key, @NotNull String[] assignments) {
        private ParsedState {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(assignments, "assignments");
        }
    }
    // #endif
}
