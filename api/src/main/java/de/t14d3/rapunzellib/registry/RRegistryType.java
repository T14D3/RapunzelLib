package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RNative;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface RRegistryType extends RNative {
    @NotNull RKey key();

    default boolean is(@NotNull RKey key) {
        return key().equals(Objects.requireNonNull(key, "key"));
    }

    default boolean is(@NotNull String key) {
        return is(RKey.of(key));
    }
}
