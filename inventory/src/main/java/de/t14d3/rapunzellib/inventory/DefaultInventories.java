package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link Inventories} that delegates wrapping to a list
 * of {@link InventoryWrapperFactory} instances.
 * <p>
 * Iterates through the registered factories in order and returns the first
 * successful wrap. Already-wrapped {@link RInventory} instances are returned directly.
 */
final class DefaultInventories implements Inventories {
    private final PlatformId platformId;
    private final List<InventoryWrapperFactory<?>> wrapperFactories;

    /**
     * Constructs a new {@code DefaultInventories} instance.
     *
     * @param platformId       the platform identifier
     * @param wrapperFactories the list of wrapper factories to delegate to
     */
    DefaultInventories(@NotNull PlatformId platformId, @NotNull List<? extends InventoryWrapperFactory<?>> wrapperFactories) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        this.wrapperFactories = List.copyOf(Objects.requireNonNull(wrapperFactories, "wrapperFactories"));
    }

    @Override
    public @NotNull PlatformId platformId() {
        return platformId;
    }

    @Override
    public @NotNull Optional<RInventory> wrap(@Nullable Object nativeInventory) {
        if (nativeInventory == null) {
            return Optional.empty();
        }
        if (nativeInventory instanceof RInventory inventory) {
            return Optional.of(inventory);
        }

        for (InventoryWrapperFactory<?> wrapperFactory : wrapperFactories) {
            if (wrapperFactory.supports(nativeInventory)) {
                return Optional.of(wrapperFactory.wrapNative(nativeInventory));
            }
        }
        return Optional.empty();
    }
}
