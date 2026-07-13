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
     * @param registryKey the key identifying the registry
     * @param <T>         the value type stored in the registry
     * @return an {@link Optional} containing the registry, or empty if not found
     */
    <T> @NotNull Optional<RRegistry<T>> findRegistry(@NotNull RRegistryKey<T> registryKey);

    /** Checks whether a registry with the given key exists. */
    default <T> boolean containsRegistry(@NotNull RRegistryKey<T> registryKey) {
        return findRegistry(registryKey).isPresent();
    }

    /** Requires a registry by key, throwing if not found. */
    default <T> @NotNull RRegistry<T> registry(@NotNull RRegistryKey<T> registryKey) {
        RRegistryKey<T> requestedRegistryKey = Objects.requireNonNull(registryKey, "registryKey");
        return findRegistry(requestedRegistryKey).orElseThrow(() -> new IllegalStateException(
            "Registry not registered: " + requestedRegistryKey.key()
        ));
    }

    /** Finds a value in a specific registry by key. */
    default <T> @NotNull Optional<T> find(@NotNull RRegistryKey<T> registryKey, @NotNull RKey key) {
        Objects.requireNonNull(registryKey, "registryKey");
        Objects.requireNonNull(key, "key");
        return findRegistry(registryKey).flatMap(registry -> registry.find(key));
    }

    /** Finds a value by registry reference. */
    default <T> @NotNull Optional<T> find(@NotNull RRegistryRef<T> ref) {
        RRegistryRef<T> requestedRef = Objects.requireNonNull(ref, "ref");
        return find(requestedRef.registryKey(), requestedRef.key());
    }

    /** Requires a value in a specific registry by key, throwing if not found. */
    default <T> @NotNull T require(@NotNull RRegistryKey<T> registryKey, @NotNull RKey key) {
        return registry(registryKey).require(key);
    }

    /** Requires a value by registry reference, throwing if not found. */
    default <T> @NotNull T require(@NotNull RRegistryRef<T> ref) {
        RRegistryRef<T> requestedRef = Objects.requireNonNull(ref, "ref");
        return require(requestedRef.registryKey(), requestedRef.key());
    }

    /** Returns all known registry keys. */
    @NotNull List<RRegistryKey<?>> registryKeys();
}
