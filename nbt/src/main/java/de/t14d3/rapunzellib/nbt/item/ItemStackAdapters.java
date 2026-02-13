package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface ItemStackAdapters {
    @NotNull PlatformId platformId();

    <T> @NotNull Optional<ItemStackAdapter<T>> find(@NotNull Class<T> handleType);

    <T> @NotNull ItemStackAdapter<T> require(@NotNull Class<T> handleType);

    @NotNull Optional<ItemStackAdapter<Object>> find(@NotNull Object handle);

    @NotNull ItemStackAdapter<Object> require(@NotNull Object handle);

    @NotNull List<Class<?>> handleTypes();
}
