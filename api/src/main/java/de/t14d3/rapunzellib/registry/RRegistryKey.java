package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

public record RRegistryKey<T>(@NotNull RKey key, @NotNull Class<T> valueType) implements Serializable {
    public RRegistryKey {
        key = Objects.requireNonNull(key, "key");
        valueType = Objects.requireNonNull(valueType, "valueType");
    }

    public static <T> @NotNull RRegistryKey<T> of(@NotNull RKey key, @NotNull Class<T> valueType) {
        return new RRegistryKey<>(key, valueType);
    }

    public static <T> @NotNull RRegistryKey<T> of(@NotNull String key, @NotNull Class<T> valueType) {
        return of(RKey.of(key), valueType);
    }

    public boolean supports(@NotNull Class<?> requestedType) {
        return Objects.requireNonNull(requestedType, "requestedType").isAssignableFrom(valueType);
    }

    public @NotNull RRegistryRef<T> ref(@NotNull RKey valueKey) {
        return RRegistryRef.of(this, valueKey);
    }

    public @NotNull RRegistryRef<T> ref(@NotNull String valueKey) {
        return ref(RKey.of(valueKey));
    }
}
