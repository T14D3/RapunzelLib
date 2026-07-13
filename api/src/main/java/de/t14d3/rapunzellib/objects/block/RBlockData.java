package de.t14d3.rapunzellib.objects.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Immutable block state data wrapping a native platform block state.
 *
 * <p>Provides access to the block type and a string representation of the full
 * block state including properties.</p>
 */
public interface RBlockData extends RNative {
    /**
     * Returns the registry reference for this block data's type.
     *
     * @return the block type reference
     */
    @NotNull RRegistryRef<RBlockType> typeRef();

    /**
     * Returns the key of this block data's type.
     *
     * @return the type key
     */
    default @NotNull RKey typeKey() {
        return typeRef().key();
    }

    /**
     * Returns the string representation of this block data's type key.
     *
     * @return the type ID string
     */
    default @NotNull String typeId() {
        return typeKey().asString();
    }

    /**
     * Resolves the block type from the type reference or registry.
     *
     * @return an {@link Optional} containing the block type, or empty if not found
     */
    default @NotNull Optional<RBlockType> type() {
        try {
            return typeRef().find();
        } catch (IllegalStateException ignored) {
            return Rapunzel.blockTypes().find(typeKey());
        }
    }

    /**
     * Resolves the block type, throwing if not found.
     *
     * @return the block type
     */
    default @NotNull RBlockType requireType() {
        try {
            return typeRef().require();
        } catch (IllegalStateException ignored) {
            return Rapunzel.blockTypes().require(typeKey());
        }
    }

    /**
     * Returns the string representation of this block data (e.g. {@code minecraft:oak_log[axis=y]}).
     *
     * @return the block data string
     */
    @NotNull String asString();

    /**
     * Wraps a native platform block data object into an RBlockData, if supported.
     *
     * @param nativeBlockData the native block data object
     * @return an {@link Optional} containing the wrapped block data, or empty if not supported
     */
    static @NotNull Optional<RBlockData> wrap(@NotNull Object nativeBlockData) {
        return Rapunzel.blocks().wrapData(nativeBlockData);
    }

    /**
     * Parses a string representation of block data into an RBlockData, if supported.
     *
     * @param value the string value to parse
     * @return an {@link Optional} containing the parsed block data, or empty if parsing fails
     */
    static @NotNull Optional<RBlockData> parse(@NotNull String value) {
        return Rapunzel.blocks().parseData(value);
    }

    /**
     * Parses a string representation of block data, throwing if parsing fails.
     *
     * @param value the string value to parse
     * @return the parsed block data
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    static @NotNull RBlockData require(@NotNull String value) {
        return parse(value).orElseThrow(() -> new IllegalArgumentException("Unknown block data: " + value));
    }
}
