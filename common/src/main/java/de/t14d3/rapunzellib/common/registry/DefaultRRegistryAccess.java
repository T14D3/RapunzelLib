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
    private final Object lock = new Object();
    private final LinkedHashMap<RKey, RegisteredRegistry<?>> registries = new LinkedHashMap<>();

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
