package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RNative;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A type entry stored in a registry, identified by a unique {@link RKey}.
 */
public interface RRegistryType extends RNative {
    /**
     * Returns the unique key identifying this type.
     *
     * @return the type key
     */
    @NotNull RKey key();

    /**
     * Checks whether this type matches the given key.
     *
     * @param key the key to compare
     * @return true if the keys are equal
     */
    default boolean is(@NotNull RKey key) {
        return key().equals(Objects.requireNonNull(key, "key"));
    }

    /**
     * Checks whether this type matches the given string key.
     *
     * @param key the key string to compare
     * @return true if the keys are equal
     */
    default boolean is(@NotNull String key) {
        return is(RKey.of(key));
    }
}
