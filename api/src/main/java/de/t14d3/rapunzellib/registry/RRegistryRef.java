package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * A reference to a specific value in a specific registry.
 *
 * <p>Combines a {@link RRegistryKey} and a value {@link RKey} for convenient
 * lookup of typed values through {@link RRegistryAccess}.</p>
 *
 * @param registryKey the registry key
 * @param key         the value key within the registry
 * @param <T>         the value type
 */
public record RRegistryRef<T>(@NotNull RRegistryKey<T> registryKey, @NotNull RKey key) implements Serializable {
    public RRegistryRef {
        registryKey = Objects.requireNonNull(registryKey, "registryKey");
        key = Objects.requireNonNull(key, "key");
    }

    /**
     * Creates a registry reference from a registry key and value key.
     *
     * @param registryKey the registry key
     * @param key         the value key
     * @param <T>         the value type
     * @return the registry reference
     */
    public static <T> @NotNull RRegistryRef<T> of(@NotNull RRegistryKey<T> registryKey, @NotNull RKey key) {
        return new RRegistryRef<>(registryKey, key);
    }

    /**
     * Creates a registry reference from a registry key and string value key.
     *
     * @param registryKey the registry key
     * @param key         the value key string
     * @param <T>         the value type
     * @return the registry reference
     */
    public static <T> @NotNull RRegistryRef<T> of(@NotNull RRegistryKey<T> registryKey, @NotNull String key) {
        return of(registryKey, RKey.of(key));
    }

    /**
     * Resolves this reference using the given registry access.
     *
     * @param registries the registry access to use
     * @return an {@link Optional} containing the value, or empty if not found
     */
    public @NotNull Optional<T> find(@NotNull RRegistryAccess registries) {
        return Objects.requireNonNull(registries, "registries").find(this);
    }

    /**
     * Resolves this reference using the given registry access, throwing if not found.
     *
     * @param registries the registry access to use
     * @return the value
     * @throws IllegalStateException if the value is not found
     */
    public @NotNull T require(@NotNull RRegistryAccess registries) {
        return Objects.requireNonNull(registries, "registries").require(this);
    }

    /**
     * Resolves this reference using the global registry access.
     *
     * @return an {@link Optional} containing the value, or empty if not found
     */
    public @NotNull Optional<T> find() {
        return Rapunzel.registries().find(this);
    }

    /**
     * Resolves this reference using the global registry access, throwing if not found.
     *
     * @return the value
     * @throws IllegalStateException if the value is not found
     */
    public @NotNull T require() {
        return Rapunzel.registries().require(this);
    }
}
