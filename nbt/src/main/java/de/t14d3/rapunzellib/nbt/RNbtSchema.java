package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A named NBT schema describing a set of typed fields that a compound is expected to contain.
 * <p>
 * Schemas are used for documentation, validation, and code generation purposes.</p>
 *
 * @param name   the schema name
 * @param fields the fields defined by this schema
 */
public record RNbtSchema(@NotNull String name, @NotNull List<RNbtField<?>> fields) implements Serializable {
    public RNbtSchema {
        Objects.requireNonNull(name, "name");
        fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    }

    /**
     * Creates a schema from a name and varargs fields.
     *
     * @param name   the schema name
     * @param fields the fields
     * @return a new schema
     */
    public static @NotNull RNbtSchema of(@NotNull String name, RNbtField<?> @NotNull ... fields) {
        return new RNbtSchema(name, Arrays.asList(fields));
    }
}
