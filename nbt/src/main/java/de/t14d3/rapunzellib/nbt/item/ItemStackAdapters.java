package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Registry of {@link ItemStackAdapter} instances keyed by native handle type, scoped to a platform.
 */
public interface ItemStackAdapters {
    /**
     * Returns the platform ID this adapter collection belongs to.
     *
     * @return the platform ID
     */
    @NotNull PlatformId platformId();

    /**
     * Finds an adapter for the given handle type.
     *
     * @param <T>        the handle type
     * @param handleType the handle class
     * @return an Optional containing the adapter, or empty if not found
     */
    <T> @NotNull Optional<ItemStackAdapter<T>> find(@NotNull Class<T> handleType);

    /**
     * Requires an adapter for the given handle type, throwing if absent.
     *
     * @param <T>        the handle type
     * @param handleType the handle class
     * @return the adapter
     * @throws IllegalStateException if no adapter is registered
     */
    <T> @NotNull ItemStackAdapter<T> require(@NotNull Class<T> handleType);

    /**
     * Finds an adapter capable of handling the given native item stack object.
     *
     * @param handle the native item stack
     * @return an Optional containing the adapter, or empty if not found
     */
    @NotNull Optional<ItemStackAdapter<Object>> find(@NotNull Object handle);

    /**
     * Requires an adapter for the given native item stack object, throwing if absent.
     *
     * @param handle the native item stack
     * @return the adapter
     * @throws IllegalStateException if no suitable adapter is registered
     */
    @NotNull ItemStackAdapter<Object> require(@NotNull Object handle);

    /**
     * Returns the list of all registered handle types.
     *
     * @return the handle type classes
     */
    @NotNull List<Class<?>> handleTypes();
}
