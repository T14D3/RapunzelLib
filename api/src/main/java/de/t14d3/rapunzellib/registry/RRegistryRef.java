package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public record RRegistryRef<T>(@NotNull RRegistryKey<T> registryKey, @NotNull RKey key) implements Serializable {
    public RRegistryRef {
        registryKey = Objects.requireNonNull(registryKey, "registryKey");
        key = Objects.requireNonNull(key, "key");
    }

    public static <T> @NotNull RRegistryRef<T> of(@NotNull RRegistryKey<T> registryKey, @NotNull RKey key) {
        return new RRegistryRef<>(registryKey, key);
    }

    public static <T> @NotNull RRegistryRef<T> of(@NotNull RRegistryKey<T> registryKey, @NotNull String key) {
        return of(registryKey, RKey.of(key));
    }

    public @NotNull Optional<T> find(@NotNull RRegistryAccess registries) {
        return Objects.requireNonNull(registries, "registries").find(this);
    }

    public @NotNull T require(@NotNull RRegistryAccess registries) {
        return Objects.requireNonNull(registries, "registries").require(this);
    }

    public @NotNull Optional<T> find() {
        return Rapunzel.registries().find(this);
    }

    public @NotNull T require() {
        return Rapunzel.registries().require(this);
    }
}
