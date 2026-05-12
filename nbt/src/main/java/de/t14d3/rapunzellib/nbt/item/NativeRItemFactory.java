package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for creating platform-native {@link NativeRItem} instances.
 * <p>
 * Platform implementations register a factory via the service registry to provide
 * native item stack wrappers backed by the server's own item representation.</p>
 */
@FunctionalInterface
public interface NativeRItemFactory {
    /**
     * Creates a native item wrapper.
     *
     * @param typeKey the item type key
     * @param amount  the stack amount
     * @param data    the NBT data
     * @return the native item wrapper
     */
    @NotNull NativeRItem<?> create(@NotNull RKey typeKey, int amount, @NotNull RNbtCompound data);
}
