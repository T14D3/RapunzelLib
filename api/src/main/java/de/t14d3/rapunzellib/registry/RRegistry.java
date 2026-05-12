package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A registry that maps {@link RKey} identifiers to typed values.
 *
 * @param <T> the value type stored in this registry
 */
public interface RRegistry<T> extends Iterable<T> {
    /**
     * Returns the key identifying this registry.
     *
     * @return the registry key
     */
    @NotNull RRegistryKey<T> registryKey();

    /**
     * Finds the first value associated with the given key, if present.
     *
     * @param key the registry key to look up
     * @return an {@link Optional} containing the value, or empty if not found
     */
    @NotNull Optional<T> find(@NotNull RKey key);

    /**
     * Finds the first value associated with the given string key, if present.
     *
     * @param key the registry key string to look up
     * @return an {@link Optional} containing the value, or empty if not found
     */
    default @NotNull Optional<T> find(@NotNull String key) {
        return find(RKey.of(key));
    }

    /**
     * Checks whether the registry contains a value for the given key.
     *
     * @param key the key to check
     * @return true if the key is registered
     */
    default boolean contains(@NotNull RKey key) {
        return find(key).isPresent();
    }

    /**
     * Checks whether the registry contains a value for the given string key.
     *
     * @param key the key string to check
     * @return true if the key is registered
     */
    default boolean contains(@NotNull String key) {
        return contains(RKey.of(key));
    }

    /**
     * Requires a value for the given key, throwing if not found.
     *
     * @param key the key to look up
     * @return the value
     * @throws IllegalArgumentException if the key is not found
     */
    default @NotNull T require(@NotNull RKey key) {
        RKey requestedKey = Objects.requireNonNull(key, "key");
        return find(requestedKey).orElseThrow(() -> new IllegalArgumentException(
            "Unknown entry " + requestedKey + " in registry " + registryKey().key()
        ));
    }

    /**
     * Requires a value for the given string key, throwing if not found.
     *
     * @param key the key string to look up
     * @return the value
     * @throws IllegalArgumentException if the key is not found
     */
    default @NotNull T require(@NotNull String key) {
        return require(RKey.of(key));
    }

    /**
     * Returns all entries in this registry.
     *
     * @return a list of all values
     */
    @NotNull List<T> entries();

    /**
     * Returns all keys in this registry.
     *
     * @return a list of all keys
     */
    @NotNull List<RKey> keys();

    /**
     * Returns registry references for all keys in this registry.
     *
     * @return a list of registry references
     */
    default @NotNull List<RRegistryRef<T>> refs() {
        return keys().stream().map(registryKey()::ref).toList();
    }

    @Override
    default @NotNull Iterator<T> iterator() {
        return entries().iterator();
    }
}
