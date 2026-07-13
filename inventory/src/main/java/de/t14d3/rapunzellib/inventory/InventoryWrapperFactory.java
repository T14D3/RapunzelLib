package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A factory that can detect whether it supports a given native inventory handle
 * and produce an {@link RInventory} wrapper around it.
 * <p>
 * Each factory is bound to a specific platform and handle type.
 *
 * @param <H> the native handle type this factory wraps
 */
public interface InventoryWrapperFactory<H> {

    @NotNull PlatformId platformId();

    @NotNull Class<H> handleType();

    /**
     * Checks whether this factory can wrap the given native inventory object.
     *
     * @param nativeInventory the native inventory to test, may be null
     * @return true if the object is an instance of the expected handle type
     */
    default boolean supports(@Nullable Object nativeInventory) {
        return nativeInventory != null && handleType().isInstance(nativeInventory);
    }

    /**
     * Wraps a native inventory handle of the expected type into an {@link RInventory}.
     *
     * @param nativeInventory the native inventory handle
     * @return the wrapped RInventory
     */
    @NotNull RInventory wrap(@NotNull H nativeInventory);

    /**
     * Wraps a native inventory handle given as a raw {@link Object}, casting it
     * to the expected handle type first.
     *
     * @param nativeInventory the native inventory handle as a generic object
     * @return the wrapped RInventory
     */
    default @NotNull RInventory wrapNative(@NotNull Object nativeInventory) {
        return wrap(handleType().cast(nativeInventory));
    }
}
