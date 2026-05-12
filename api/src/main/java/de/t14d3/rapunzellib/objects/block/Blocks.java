package de.t14d3.rapunzellib.objects.block;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Provides block wrapping, data access, and lookup operations.
 */
public interface Blocks {
    /**
     * Wraps a native platform block object into an {@link RBlock}, if supported.
     *
     * @param nativeBlock the native block object
     * @return an {@link Optional} containing the wrapped block, or empty if not supported
     */
    @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock);

    /**
     * Wraps a native block object and casts it to the requested type.
     *
     * @param nativeBlock the native block object
     * @param blockType   the expected block type class
     * @param <T>         the block type
     * @return an {@link Optional} containing the wrapped and typed block, or empty if not applicable
     */
    default <T extends RBlock> @NotNull Optional<T> wrap(@NotNull Object nativeBlock, @NotNull Class<T> blockType) {
        Objects.requireNonNull(nativeBlock, "nativeBlock");
        Objects.requireNonNull(blockType, "blockType");
        return wrap(nativeBlock).filter(blockType::isInstance).map(blockType::cast);
    }

    /**
     * Wraps a native block data object into an {@link RBlockData}, if supported.
     *
     * @param nativeBlockData the native block data object
     * @return an {@link Optional} containing the wrapped block data, or empty if not supported
     */
    @NotNull Optional<RBlockData> wrapData(@NotNull Object nativeBlockData);

    /**
     * Returns the block at the given position in the given world.
     *
     * @param world the world
     * @param pos   the block position
     * @return the block at the position
     */
    @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos);

    /**
     * Parses a string representation of block data (e.g. from a snapshot).
     *
     * @param value the string value to parse
     * @return an {@link Optional} containing the parsed block data, or empty if parsing fails
     */
    @NotNull Optional<RBlockData> parseData(@NotNull String value);

    /**
     * Requires wrapping a native block object, throwing if not possible.
     *
     * @param nativeBlock the native block object
     * @return the wrapped block
     * @throws IllegalArgumentException if wrapping is not supported
     */
    default @NotNull RBlock require(@NotNull Object nativeBlock) {
        return wrap(nativeBlock).orElseThrow(() -> new IllegalArgumentException("Cannot wrap block: " + nativeBlock));
    }

    /**
     * Requires wrapping a native block into the specified type, throwing if not possible.
     *
     * @param nativeBlock the native block object
     * @param blockType   the expected block type class
     * @param <T>         the block type
     * @return the wrapped and typed block
     * @throws IllegalArgumentException if wrapping is not supported
     */
    default <T extends RBlock> @NotNull T require(@NotNull Object nativeBlock, @NotNull Class<T> blockType) {
        Objects.requireNonNull(blockType, "blockType");
        return wrap(nativeBlock, blockType)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap " + blockType.getSimpleName() + ": " + nativeBlock));
    }

    /**
     * Requires wrapping a native block data object, throwing if not possible.
     *
     * @param nativeBlockData the native block data object
     * @return the wrapped block data
     * @throws IllegalArgumentException if wrapping is not supported
     */
    default @NotNull RBlockData requireData(@NotNull Object nativeBlockData) {
        return wrapData(nativeBlockData)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap block data: " + nativeBlockData));
    }
}
