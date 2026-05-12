package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Static helper for wrapping native inventory objects via the {@link InventoryInterop} service.
 */
public final class InventoryInteropSupport {
    private InventoryInteropSupport() {
    }

    /**
     * Wraps a native inventory object using the available {@link InventoryInterop} service.
     *
     * @param nativeInventory the native inventory object, may be null
     * @return an {@link Optional} containing the wrapped container, or empty if no interop service is available
     */
    public static @NotNull Optional<RContainer> wrapInventory(@Nullable Object nativeInventory) {
        if (nativeInventory == null) {
            return Optional.empty();
        }

        return Rapunzel.findContext()
            .flatMap(context -> context.services().find(InventoryInterop.class))
            .flatMap(interop -> interop.wrapInventory(nativeInventory));
    }

    /**
     * Wraps a native inventory object and casts it to the requested type.
     *
     * @param nativeInventory the native inventory object, may be null
     * @param type            the expected container type class
     * @param <T>             the container type
     * @return an {@link Optional} containing the wrapped and typed container, or empty if not applicable
     */
    public static <T extends RContainer> @NotNull Optional<T> wrapInventory(@Nullable Object nativeInventory, @NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        return wrapInventory(nativeInventory)
            .filter(type::isInstance)
            .map(type::cast);
    }
}
