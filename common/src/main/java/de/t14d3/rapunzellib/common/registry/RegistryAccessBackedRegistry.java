package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RRegistry;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.registry.RRegistryKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A registry implementation that delegates all operations to a backing {@link RRegistryAccess}.
 * <p>
 * Provides a bridge between access key-based registry lookup and the direct
 * {@link RRegistry} interface by delegating find, entries, and keys calls to
 * the underlying registry access.
 *
 * @param <T> the registry value type
 */
public abstract class RegistryAccessBackedRegistry<T> implements RRegistry<T> {
    /** The backing registry access */
    private final RRegistryAccess registries;
    /** The registry key identifying this registry */
    private final RRegistryKey<T> registryKey;

    /**
     * Creates a registry backed by the given access and key.
     *
     * @param registries  the backing registry access
     * @param registryKey the registry key
     */
    protected RegistryAccessBackedRegistry(@NotNull RRegistryAccess registries, @NotNull RRegistryKey<T> registryKey) {
        this.registries = Objects.requireNonNull(registries, "registries");
        this.registryKey = Objects.requireNonNull(registryKey, "registryKey");
    }

    /**
     * Gets the registry key.
     *
     * @return the registry key
     */
    @Override
    public final @NotNull RRegistryKey<T> registryKey() {
        return registryKey;
    }

    /**
     * Finds a value by key.
     *
     * @param key the lookup key
     * @return an optional containing the value, or empty if not found
     */
    @Override
    public final @NotNull Optional<T> find(@NotNull RKey key) {
        return delegate().find(key);
    }

    /**
     * Returns all entries in this registry.
     *
     * @return an immutable list of entries
     */
    @Override
    public final @NotNull List<T> entries() {
        return delegate().entries();
    }

    /**
     * Returns all keys in this registry.
     *
     * @return an immutable list of keys
     */
    @Override
    public final @NotNull List<RKey> keys() {
        return delegate().keys();
    }

    /**
     * Resolves the actual registry from the backing registry access.
     *
     * @return the delegate registry
     */
    protected final @NotNull RRegistry<T> delegate() {
        return registries.registry(registryKey);
    }
}
