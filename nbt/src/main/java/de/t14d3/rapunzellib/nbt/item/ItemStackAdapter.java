package de.t14d3.rapunzellib.nbt.item;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ItemStackAdapter<T> {

    @NotNull RItem snapshot(@NotNull T nativeItem);

    @NotNull T create(@NotNull RItem item);

    @NotNull T apply(@NotNull T nativeItem, @NotNull RItem item);

    boolean supports(@Nullable Object object);
}
