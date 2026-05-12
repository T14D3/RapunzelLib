package de.t14d3.rapunzellib.nbt.item;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adapter interface for converting between native platform item stacks and {@link RItem} snapshots.
 *
 * @param <T> the native item stack handle type
 */
public interface ItemStackAdapter<T> {

    /**
     * Takes a snapshot of a native item stack as an immutable {@link RItem}.
     *
     * @param nativeItem the native item stack
     * @return the snapshot
     */
    @NotNull RItem snapshot(@NotNull T nativeItem);

    /**
     * Creates a new native item stack from the given {@link RItem} snapshot.
     *
     * @param item the item descriptor
     * @return the new native item stack
     */
    @NotNull T create(@NotNull RItem item);

    /**
     * Applies the properties from the given {@link RItem} to an existing native item stack.
     *
     * @param nativeItem the native item stack to modify
     * @param item       the item descriptor
     * @return the modified native item stack
     */
    @NotNull T apply(@NotNull T nativeItem, @NotNull RItem item);

    /**
     * Checks whether this adapter supports the given object (e.g. an item stack instance).
     *
     * @param object the object to test
     * @return true if this adapter can handle the object
     */
    boolean supports(@Nullable Object object);
}
