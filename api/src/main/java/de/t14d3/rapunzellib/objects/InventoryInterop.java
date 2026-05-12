package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Service interface for wrapping native platform inventory objects into RapunzelLib containers.
 */
public interface InventoryInterop {
    /**
     * Wraps a native inventory object into an {@link RContainer}, if supported.
     *
     * @param nativeInventory the native inventory object, may be null
     * @return an {@link Optional} containing the wrapped container, or empty if not supported
     */
    @NotNull Optional<RContainer> wrapInventory(@Nullable Object nativeInventory);

    /**
     * Checks whether the given native object can be wrapped as an inventory.
     *
     * @param nativeInventory the native inventory object, may be null
     * @return true if wrapping is supported
     */
    default boolean supportsInventory(@Nullable Object nativeInventory) {
        return wrapInventory(nativeInventory).isPresent();
    }

    /**
     * Wraps a native inventory object into an {@link RContainer}, throwing if not possible.
     *
     * @param nativeInventory the native inventory object
     * @return the wrapped container
     * @throws IllegalArgumentException if wrapping is not supported
     */
    default @NotNull RContainer requireInventory(@NotNull Object nativeInventory) {
        Objects.requireNonNull(nativeInventory, "nativeInventory");
        return wrapInventory(nativeInventory)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap inventory: " + nativeInventory));
    }
}
