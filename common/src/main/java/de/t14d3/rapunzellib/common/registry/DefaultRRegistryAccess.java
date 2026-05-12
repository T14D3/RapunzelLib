package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RRegistry;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.registry.RRegistryKey;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link RRegistryAccess}.
 * <p>
 * Stores registries in a linked hash map keyed by their {@link RRegistryKey} for
 * insertion-order iteration. Provides registration with key validation and
 * type-safe lookup.
 */
public final class DefaultRRegistryAccess implements RRegistryAccess {
    /** Synchronization lock */
    private final Object lock = new Object();
    /** Registered registries keyed by their registry key */
    private final LinkedHashMap<RKey, RegisteredRegistry<?>> registries = new LinkedHashMap<>();

    /**
     * Registers a registry, validating that its reported key matches the requested key.
     *
     * @param registryKey the registry key
     * @param registry    the registry to register
     * @param <T>         the registry value type
     * @throws IllegalArgumentException if the registry's key does not match
     * @throws IllegalStateException    if the key is already registered with a different value type
     */
    public <T> void register(@NotNull RRegistryKey<T> registryKey, @NotNull RRegistry<T> registry) {
        RRegistryKey<T> requestedRegistryKey = Objects.requireNonNull(registryKey, "registryKey");
        RRegistry<T> requestedRegistry = Objects.requireNonNull(registry, "registry");
        if (!requestedRegistry.registryKey().equals(requestedRegistryKey)) {
            throw new IllegalArgumentException(
                "Registry " + requestedRegistry.getClass().getName()
                    + " reports key " + requestedRegistry.registryKey().key()
                    + " but was registered as " + requestedRegistryKey.key()
            );
        }

        synchronized (lock) {
            RegisteredRegistry<?> existing = registries.get(requestedRegistryKey.key());
            if (existing != null && !existing.registryKey.equals(requestedRegistryKey)) {
                throw new IllegalStateException(
                    "Registry key " + requestedRegistryKey.key() + " is already registered with value type "
                        + existing.registryKey.valueType().getName()
                );
            }
            registries.put(requestedRegistryKey.key(), new RegisteredRegistry<>(requestedRegistryKey, requestedRegistry));
        }
    }

    /**
     * Registers a registry if no registration exists for the key.
     *
     * @param registryKey the registry key
     * @param registry    the registry to register
     * @param <T>         the registry value type
     * @return the existing or newly registered registry
     */
    public <T> @NotNull RRegistry<T> registerIfAbsent(@NotNull RRegistryKey<T> registryKey, @NotNull RRegistry<T> registry) {
        Objects.requireNonNull(registryKey, "registryKey");
        Objects.requireNonNull(registry, "registry");

        synchronized (lock) {
            Optional<RRegistry<T>> existing = findRegistry(registryKey);
            if (existing.isPresent()) {
                return existing.orElseThrow();
            }
            register(registryKey, registry);
            return registry;
        }
    }

    /**
     * Finds a registry by its key.
     *
     * @param registryKey the registry key
     * @param <T>         the registry value type
     * @return an optional containing the registry, or empty if not found
     * @throws IllegalStateException if the key is found but with a different value type
     */
    @Override
    public <T> @NotNull Optional<RRegistry<T>> findRegistry(@NotNull RRegistryKey<T> registryKey) {
        RRegistryKey<T> requestedRegistryKey = Objects.requireNonNull(registryKey, "registryKey");
        synchronized (lock) {
            RegisteredRegistry<?> existing = registries.get(requestedRegistryKey.key());
            if (existing == null) {
                return Optional.empty();
            }
            if (!existing.registryKey.equals(requestedRegistryKey)) {
                throw new IllegalStateException(
                    "Registry key " + requestedRegistryKey.key() + " is registered for "
                        + existing.registryKey.valueType().getName() + " but was requested as "
                        + requestedRegistryKey.valueType().getName()
                );
            }
            return Optional.of(castRegistry(existing.registry()));
        }
    }

    /**
     * Returns all registered registry keys.
     *
     * @return an immutable list of registry keys
     */
    @Override
    public @NotNull List<RRegistryKey<?>> registryKeys() {
        synchronized (lock) {
            return List.copyOf(registries.values().stream().map(registry -> (RRegistryKey<?>) registry.registryKey()).toList());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> RRegistry<T> castRegistry(RRegistry<?> registry) {
        return (RRegistry<T>) registry;
    }

    private record RegisteredRegistry<T>(RRegistryKey<T> registryKey, RRegistry<T> registry) {
    }
}
