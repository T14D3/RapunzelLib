package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.InventoryInterop;
import de.t14d3.rapunzellib.objects.RContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public interface Inventories extends InventoryInterop {
    @NotNull PlatformId platformId();

    @NotNull Optional<RInventory> wrap(@Nullable Object nativeInventory);

    @Override
    default @NotNull Optional<RContainer> wrapInventory(@Nullable Object nativeInventory) {
        return wrap(nativeInventory).map(RContainer.class::cast);
    }

    @Override
    default @NotNull RContainer requireInventory(@NotNull Object nativeInventory) {
        return require(nativeInventory);
    }

    default boolean supports(@Nullable Object nativeInventory) {
        return wrap(nativeInventory).isPresent();
    }

    default @NotNull RInventory require(@NotNull Object nativeInventory) {
        Objects.requireNonNull(nativeInventory, "nativeInventory");
        return wrap(nativeInventory)
            .orElseThrow(() -> new IllegalArgumentException(
                "Cannot wrap inventory: " + nativeInventory + " for platform " + platformId()
            ));
    }
}
