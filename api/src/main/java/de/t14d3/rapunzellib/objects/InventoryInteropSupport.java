package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class InventoryInteropSupport {
    private InventoryInteropSupport() {
    }

    public static @NotNull Optional<RContainer> wrapInventory(@Nullable Object nativeInventory) {
        if (nativeInventory == null) {
            return Optional.empty();
        }

        return Rapunzel.findContext()
            .flatMap(context -> context.services().find(InventoryInterop.class))
            .flatMap(interop -> interop.wrapInventory(nativeInventory));
    }

    public static <T extends RContainer> @NotNull Optional<T> wrapInventory(@Nullable Object nativeInventory, @NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        return wrapInventory(nativeInventory)
            .filter(type::isInstance)
            .map(type::cast);
    }
}
