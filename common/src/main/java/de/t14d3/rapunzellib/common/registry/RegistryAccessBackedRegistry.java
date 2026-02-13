package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RRegistry;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.registry.RRegistryKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class RegistryAccessBackedRegistry<T> implements RRegistry<T> {
    private final RRegistryAccess registries;
    private final RRegistryKey<T> registryKey;

    protected RegistryAccessBackedRegistry(@NotNull RRegistryAccess registries, @NotNull RRegistryKey<T> registryKey) {
        this.registries = Objects.requireNonNull(registries, "registries");
        this.registryKey = Objects.requireNonNull(registryKey, "registryKey");
    }

    @Override
    public final @NotNull RRegistryKey<T> registryKey() {
        return registryKey;
    }

    @Override
    public final @NotNull Optional<T> find(@NotNull RKey key) {
        return delegate().find(key);
    }

    @Override
    public final @NotNull List<T> entries() {
        return delegate().entries();
    }

    @Override
    public final @NotNull List<RKey> keys() {
        return delegate().keys();
    }

    protected final @NotNull RRegistry<T> delegate() {
        return registries.registry(registryKey);
    }
}
