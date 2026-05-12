package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides access to all named registries in the RapunzelLib registry system.
 *
 * <p>Registries are identified by {@link RRegistryKey} and store typed values
 * mapped by {@link RKey} identifiers.</p>
 */
public interface RRegistryAccess {
    /**
     * Finds a registry by its key.
     *
     * @param registryKey the registry key
     * @param <T>         the value type of the registry
     * @return an {@link Optional} containing the registry, or empty if not registered
     */
    <T> @NotNull Optional<RRegistry<T>> findRegistry(@NotNull RRegistryKey<T> registryKey);

    /**
     * Checks whether a registry with the given key exists.
     *
     * @param registryKey the registry key
     * @param <T>         the value type of the registry
     * @return true if the registry exists
     */
    default <T> boolean containsRegistry(@NotNull RRegistryKey<T> registryKey) {
        return findRegistry(registryKey).isPresent();
    }

    /**
     * Requires a registry by key, throwing if not found.
     *
     * @param registryKey the registry key
     * @param <T>         the value type of the registry
     * @return the registry
     * @throws IllegalStateException if the registry is not registered
     */
    default <T> @NotNull RRegistry<T> registry(@NotNull RRegistryKey<T> registryKey) {
        RRegistryKey<T> requestedRegistryKey = Objects.requireNonNull(registryKey, "registryKey");
        return findRegistry(requestedRegistryKey).orElseThrow(() -> new IllegalStateException(
            "Registry not registered: " + requestedRegistryKey.key()
        ));
    }

    /**
     * Finds a value in a specific registry by key.
     *
     * @param registryKey the registry to search
     * @param key         the value key
     * @param <T>         the value type
     * @return an {@link Optional} containing the value, or empty if not found
     */
    default <T> @NotNull Optional<T> find(@NotNull RRegistryKey<T> registryKey, @NotNull RKey key) {
        Objects.requireNonNull(registryKey, "registryKey");
        Objects.requireNonNull(key, "key");
        return findRegistry(registryKey).flatMap(registry -> registry.find(key));
    }

    /**
     * Finds a value by registry reference.
     *
     * @param ref the registry reference
     * @param <T> the value type
     * @return an {@link Optional} containing the value, or empty if not found
     */
    default <T> @NotNull Optional<T> find(@NotNull RRegistryRef<T> ref) {
        RRegistryRef<T> requestedRef = Objects.requireNonNull(ref, "ref");
        return find(requestedRef.registryKey(), requestedRef.key());
    }

    /**
     * Requires a value in a specific registry by key, throwing if not found.
     *
     * @param registryKey the registry to search
     * @param key         the value key
     * @param <T>         the value type
     * @return the value
     * @throws IllegalStateException if the registry or value is not found
     */
    default <T> @NotNull T require(@NotNull RRegistryKey<T> registryKey, @NotNull RKey key) {
        return registry(registryKey).require(key);
    }

    /**
     * Requires a value by registry reference, throwing if not found.
     *
     * @param ref the registry reference
     * @param <T> the value type
     * @return the value
     * @throws IllegalStateException if the value is not found
     */
    default <T> @NotNull T require(@NotNull RRegistryRef<T> ref) {
        RRegistryRef<T> requestedRef = Objects.requireNonNull(ref, "ref");
        return require(requestedRef.registryKey(), requestedRef.key());
    }

    /**
     * Returns all known registry keys.
     *
     * @return a list of all registry keys
     */
    @NotNull List<RRegistryKey<?>> registryKeys();
}
