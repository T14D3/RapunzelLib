package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface InventoryWrapperFactory<H> {
    @NotNull PlatformId platformId();

    @NotNull Class<H> handleType();

    default boolean supports(@Nullable Object nativeInventory) {
        return nativeInventory != null && handleType().isInstance(nativeInventory);
    }

    @NotNull RInventory wrap(@NotNull H nativeInventory);

    default @NotNull RInventory wrapNative(@NotNull Object nativeInventory) {
        return wrap(handleType().cast(nativeInventory));
    }
}
