package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record RNbtSchema(@NotNull String name, @NotNull List<RNbtField<?>> fields) implements Serializable {
    public RNbtSchema {
        Objects.requireNonNull(name, "name");
        fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    }

    public static @NotNull RNbtSchema of(@NotNull String name, RNbtField<?> @NotNull ... fields) {
        return new RNbtSchema(name, Arrays.asList(fields));
    }
}
