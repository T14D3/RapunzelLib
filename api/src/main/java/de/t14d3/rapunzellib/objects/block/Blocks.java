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
    /** Wraps a native platform block object into an {@link RBlock}, if supported. */
    @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock);

    /** Wraps a native block object and casts it to the requested type. */
    default <T extends RBlock> @NotNull Optional<T> wrap(@NotNull Object nativeBlock, @NotNull Class<T> blockType) {
        Objects.requireNonNull(nativeBlock, "nativeBlock");
        Objects.requireNonNull(blockType, "blockType");
        return wrap(nativeBlock).filter(blockType::isInstance).map(blockType::cast);
    }

    /** Wraps a native block data object into an {@link RBlockData}, if supported. */
    @NotNull Optional<RBlockData> wrapData(@NotNull Object nativeBlockData);

    /** Returns the block at the given position in the given world. */
    @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos);

    /** Parses a string representation of block data (e.g. from a snapshot). */
    @NotNull Optional<RBlockData> parseData(@NotNull String value);

    /** Requires wrapping a native block object, throwing if not possible. */
    default @NotNull RBlock require(@NotNull Object nativeBlock) {
        return wrap(nativeBlock).orElseThrow(() -> new IllegalArgumentException("Cannot wrap block: " + nativeBlock));
    }

    /** Requires wrapping a native block into the specified type, throwing if not possible. */
    default <T extends RBlock> @NotNull T require(@NotNull Object nativeBlock, @NotNull Class<T> blockType) {
        Objects.requireNonNull(blockType, "blockType");
        return wrap(nativeBlock, blockType)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap " + blockType.getSimpleName() + ": " + nativeBlock));
    }

    /** Requires wrapping a native block data object, throwing if not possible. */
    default @NotNull RBlockData requireData(@NotNull Object nativeBlockData) {
        return wrapData(nativeBlockData)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap block data: " + nativeBlockData));
    }
}
