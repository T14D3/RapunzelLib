package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * A named, typed NBT field descriptor that encapsulates a key and its associated {@link RNbtCodec codec}
 * and {@link RNbtPath path}. Provides type-safe read, write, and remove operations on compounds.
 *
 * @param <T> the Java type of this field
 */
public final class RNbtField<T> implements Serializable {
    private final @NotNull String key;
    private final @NotNull RNbtPath<T> path;

    private RNbtField(@NotNull String key, @NotNull RNbtCodec<T> codec) {
        this.key = Objects.requireNonNull(key, "key");
        this.path = RNbtPath.of(Objects.requireNonNull(codec, "codec"), key);
    }

    /**
     * Creates a field with the given key and codec, wrapping it into a single-key path.
     *
     * @param <T>   the value type
     * @param key   the field key
     * @param codec the codec for serialization
     * @return a new field
     */
    public static <T> @NotNull RNbtField<T> of(@NotNull String key, @NotNull RNbtCodec<T> codec) {
        return new RNbtField<>(key, codec);
    }

    /**
     * Returns the field key.
     *
     * @return the key
     */
    public @NotNull String key() {
        return key;
    }

    /**
     * Returns the underlying path for this field.
     *
     * @return the path
     */
    public @NotNull RNbtPath<T> path() {
        return path;
    }

    /**
     * Returns the codec used by this field.
     *
     * @return the codec
     */
    public @NotNull RNbtCodec<T> codec() {
        return path.codec();
    }

    /**
     * Reads the value of this field from the given compound.
     *
     * @param compound the compound to read from
     * @return an Optional containing the value, or empty if absent
     */
    public @NotNull Optional<T> read(@NotNull RNbtCompound compound) {
        return path.read(Objects.requireNonNull(compound, "compound"));
    }

    /**
     * Writes a value to this field in the given compound.
     *
     * @param compound the compound to modify
     * @param value    the value to write
     * @return a new compound with the field applied
     */
    public @NotNull RNbtCompound write(@NotNull RNbtCompound compound, @NotNull T value) {
        return path.write(Objects.requireNonNull(compound, "compound"), value);
    }

    /**
     * Removes this field from the given compound.
     *
     * @param compound the compound to modify
     * @return a new compound with the field removed
     */
    public @NotNull RNbtCompound remove(@NotNull RNbtCompound compound) {
        return path.remove(Objects.requireNonNull(compound, "compound"));
    }

    @Override
    public String toString() {
        return key;
    }
}
