package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public interface InventoryInterop {
    @NotNull Optional<RContainer> wrapInventory(@Nullable Object nativeInventory);

    default boolean supportsInventory(@Nullable Object nativeInventory) {
        return wrapInventory(nativeInventory).isPresent();
    }

    default @NotNull RContainer requireInventory(@NotNull Object nativeInventory) {
        Objects.requireNonNull(nativeInventory, "nativeInventory");
        return wrapInventory(nativeInventory)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap inventory: " + nativeInventory));
    }
}
