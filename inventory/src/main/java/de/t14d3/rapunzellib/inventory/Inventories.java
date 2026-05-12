package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.InventoryInterop;
import de.t14d3.rapunzellib.objects.RContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Service interface for wrapping native platform inventory objects into
 * Rapunzel's cross-platform {@link RInventory} abstraction.
 * <p>
 * Implementations maintain a registry of {@link InventoryWrapperFactory factories}
 * and attempt each in order when given a native inventory handle.
 */
public interface Inventories extends InventoryInterop {

    /**
     * Returns the platform this inventory service is bound to.
     *
     * @return the platform identifier
     */
    @NotNull PlatformId platformId();

    /**
     * Attempts to wrap a native inventory object into an {@link RInventory}.
     *
     * @param nativeInventory the native inventory to wrap, may be null
     * @return an {@link Optional} containing the wrapped inventory, or empty if unsupported
     */
    @NotNull Optional<RInventory> wrap(@Nullable Object nativeInventory);

    @Override
    default @NotNull Optional<RContainer> wrapInventory(@Nullable Object nativeInventory) {
        return wrap(nativeInventory).map(RContainer.class::cast);
    }

    @Override
    default @NotNull RContainer requireInventory(@NotNull Object nativeInventory) {
        return require(nativeInventory);
    }

    /**
     * Checks whether the given native inventory object can be wrapped.
     *
     * @param nativeInventory the native inventory to test, may be null
     * @return true if a matching wrapper factory exists
     */
    default boolean supports(@Nullable Object nativeInventory) {
        return wrap(nativeInventory).isPresent();
    }

    /**
     * Wraps the native inventory or throws an exception if no factory supports it.
     *
     * @param nativeInventory the native inventory to wrap
     * @return the wrapped RInventory
     * @throws IllegalArgumentException if the inventory cannot be wrapped by this service
     */
    default @NotNull RInventory require(@NotNull Object nativeInventory) {
        Objects.requireNonNull(nativeInventory, "nativeInventory");
        return wrap(nativeInventory)
            .orElseThrow(() -> new IllegalArgumentException(
                "Cannot wrap inventory: " + nativeInventory + " for platform " + platformId()
            ));
    }
}
