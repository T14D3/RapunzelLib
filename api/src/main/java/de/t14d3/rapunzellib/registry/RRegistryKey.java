package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Identifies a registry by its key and value type.
 *
 * @param key       the unique key for this registry
 * @param valueType the value type class stored in this registry
 * @param <T>       the value type
 */
public record RRegistryKey<T>(@NotNull RKey key, @NotNull Class<T> valueType) implements Serializable {
    public RRegistryKey {
        key = Objects.requireNonNull(key, "key");
        valueType = Objects.requireNonNull(valueType, "valueType");
    }

    /**
     * Creates a registry key from an {@link RKey} and value type.
     *
     * @param key       the registry key
     * @param valueType the value type class
     * @param <T>       the value type
     * @return the registry key
     */
    public static <T> @NotNull RRegistryKey<T> of(@NotNull RKey key, @NotNull Class<T> valueType) {
        return new RRegistryKey<>(key, valueType);
    }

    /**
     * Creates a registry key from a string key and value type.
     *
     * @param key       the registry key string
     * @param valueType the value type class
     * @param <T>       the value type
     * @return the registry key
     */
    public static <T> @NotNull RRegistryKey<T> of(@NotNull String key, @NotNull Class<T> valueType) {
        return of(RKey.of(key), valueType);
    }

    /**
     * Checks whether the given type is assignable from this registry's value type.
     *
     * @param requestedType the type to check
     * @return true if the registry supports the requested type
     */
    public boolean supports(@NotNull Class<?> requestedType) {
        return Objects.requireNonNull(requestedType, "requestedType").isAssignableFrom(valueType);
    }

    /**
     * Creates a registry reference for a specific value key in this registry.
     *
     * @param valueKey the value key
     * @return the registry reference
     */
    public @NotNull RRegistryRef<T> ref(@NotNull RKey valueKey) {
        return RRegistryRef.of(this, valueKey);
    }

    /**
     * Creates a registry reference for a specific value key string in this registry.
     *
     * @param valueKey the value key string
     * @return the registry reference
     */
    public @NotNull RRegistryRef<T> ref(@NotNull String valueKey) {
        return ref(RKey.of(valueKey));
    }
}
