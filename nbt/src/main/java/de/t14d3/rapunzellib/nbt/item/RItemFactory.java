package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal factory for creating platform-specific {@link RItem} instances.
 * <p>
 * Delegates to a registered {@link NativeRItemFactory} service if available,
 * falling back to {@link SimpleRItem} if no platform context is present.</p>
 */
final class RItemFactory {
    private RItemFactory() {
    }

    /**
     * Attempts to create a platform-specific item, returning null if no factory is available.
     *
     * @param typeKey the item type key
     * @param amount  the stack amount
     * @param data    the NBT data
     * @return a native item, or null
     */
    static @Nullable RItem tryCreate(@NotNull RKey typeKey, int amount, @NotNull RNbtCompound data) {
        return Rapunzel.findContext()
            .flatMap(ctx -> ctx.services().find(NativeRItemFactory.class))
            .map(factory -> factory.create(typeKey, amount, data))
            .orElse(null);
    }
}
