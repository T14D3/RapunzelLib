package de.t14d3.rapunzellib.objects;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("PatternValidation")
public record RExtraKey<T>(@NotNull Key id, @NotNull Class<T> type) {
    public RExtraKey {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
    }

    public static <T> @NotNull RExtraKey<T> of(@NotNull Key id, @NotNull Class<T> type) {
        return new RExtraKey<>(id, type);
    }

    public static <T> @NotNull RExtraKey<T> of(@NotNull String id, @NotNull Class<T> type) {
        return new RExtraKey<>(Key.key(id), type);
    }

    public static <T> @NotNull RExtraKey<T> of(@NotNull String namespace, @NotNull String value, @NotNull Class<T> type) {
        return new RExtraKey<>(Key.key(namespace, value), type);
    }
}
