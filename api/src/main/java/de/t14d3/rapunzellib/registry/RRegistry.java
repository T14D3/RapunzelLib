package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface RRegistry<T> extends Iterable<T> {
    @NotNull RRegistryKey<T> registryKey();

    @NotNull Optional<T> find(@NotNull RKey key);

    default @NotNull Optional<T> find(@NotNull String key) {
        return find(RKey.of(key));
    }

    default boolean contains(@NotNull RKey key) {
        return find(key).isPresent();
    }

    default boolean contains(@NotNull String key) {
        return contains(RKey.of(key));
    }

    default @NotNull T require(@NotNull RKey key) {
        RKey requestedKey = Objects.requireNonNull(key, "key");
        return find(requestedKey).orElseThrow(() -> new IllegalArgumentException(
            "Unknown entry " + requestedKey + " in registry " + registryKey().key()
        ));
    }

    default @NotNull T require(@NotNull String key) {
        return require(RKey.of(key));
    }

    @NotNull List<T> entries();

    @NotNull List<RKey> keys();

    default @NotNull List<RRegistryRef<T>> refs() {
        return keys().stream().map(registryKey()::ref).toList();
    }

    @Override
    default @NotNull Iterator<T> iterator() {
        return entries().iterator();
    }
}
