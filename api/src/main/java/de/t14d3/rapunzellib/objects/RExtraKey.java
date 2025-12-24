package de.t14d3.rapunzellib.objects;

import net.kyori.adventure.key.Key;

import java.util.Objects;

public record RExtraKey<T>(Key id, Class<T> type) {
    public RExtraKey {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
    }

    public static <T> RExtraKey<T> of(Key id, Class<T> type) {
        return new RExtraKey<>(id, type);
    }

    public static <T> RExtraKey<T> of(String id, Class<T> type) {
        return new RExtraKey<>(Key.key(id), type);
    }

    public static <T> RExtraKey<T> of(String namespace, String value, Class<T> type) {
        return new RExtraKey<>(Key.key(namespace, value), type);
    }
}
