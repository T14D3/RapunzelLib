package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface RRegistryAccess {
    <T> @NotNull Optional<RRegistry<T>> findRegistry(@NotNull RRegistryKey<T> registryKey);

    default <T> boolean containsRegistry(@NotNull RRegistryKey<T> registryKey) {
        return findRegistry(registryKey).isPresent();
    }

    default <T> @NotNull RRegistry<T> registry(@NotNull RRegistryKey<T> registryKey) {
        RRegistryKey<T> requestedRegistryKey = Objects.requireNonNull(registryKey, "registryKey");
        return findRegistry(requestedRegistryKey).orElseThrow(() -> new IllegalStateException(
            "Registry not registered: " + requestedRegistryKey.key()
        ));
    }

    default <T> @NotNull Optional<T> find(@NotNull RRegistryKey<T> registryKey, @NotNull RKey key) {
        Objects.requireNonNull(registryKey, "registryKey");
        Objects.requireNonNull(key, "key");
        return findRegistry(registryKey).flatMap(registry -> registry.find(key));
    }

    default <T> @NotNull Optional<T> find(@NotNull RRegistryRef<T> ref) {
        RRegistryRef<T> requestedRef = Objects.requireNonNull(ref, "ref");
        return find(requestedRef.registryKey(), requestedRef.key());
    }

    default <T> @NotNull T require(@NotNull RRegistryKey<T> registryKey, @NotNull RKey key) {
        return registry(registryKey).require(key);
    }

    default <T> @NotNull T require(@NotNull RRegistryRef<T> ref) {
        RRegistryRef<T> requestedRef = Objects.requireNonNull(ref, "ref");
        return require(requestedRef.registryKey(), requestedRef.key());
    }

    @NotNull List<RRegistryKey<?>> registryKeys();
}
