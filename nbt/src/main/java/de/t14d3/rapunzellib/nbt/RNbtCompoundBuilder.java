package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A mutable builder for constructing {@link RNbtCompound} instances fluently.
 * <p>
 * Provides convenience methods for all primitive NBT types.</p>
 */
public final class RNbtCompoundBuilder {
    private final LinkedHashMap<String, RNbtValue> entries = new LinkedHashMap<>();

    /**
     * Adds a key-value pair to the builder.
     *
     * @param key   the key
     * @param value the NBT value
     * @return this builder
     */
    public RNbtCompoundBuilder put(@NotNull String key, @NotNull RNbtValue value) {
        entries.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        return this;
    }

    /**
     * Adds all entries from the given map.
     *
     * @param values the map of entries
     * @return this builder
     */
    public RNbtCompoundBuilder putAll(@NotNull Map<String, ? extends RNbtValue> values) {
        Objects.requireNonNull(values, "values").forEach(this::put);
        return this;
    }

    /**
     * Adds a string entry.
     *
     * @param key   the key
     * @param value the string value
     * @return this builder
     */
    public RNbtCompoundBuilder putString(@NotNull String key, @NotNull String value) {
        return put(key, RNbtPrimitive.ofString(value));
    }

    /**
     * Adds a byte entry.
     *
     * @param key   the key
     * @param value the byte value
     * @return this builder
     */
    public RNbtCompoundBuilder putByte(@NotNull String key, byte value) {
        return put(key, RNbtPrimitive.ofByte(value));
    }

    /**
     * Adds a short entry.
     *
     * @param key   the key
     * @param value the short value
     * @return this builder
     */
    public RNbtCompoundBuilder putShort(@NotNull String key, short value) {
        return put(key, RNbtPrimitive.ofShort(value));
    }

    /**
     * Adds an int entry.
     *
     * @param key   the key
     * @param value the int value
     * @return this builder
     */
    public RNbtCompoundBuilder putInt(@NotNull String key, int value) {
        return put(key, RNbtPrimitive.ofInt(value));
    }

    /**
     * Adds a long entry.
     *
     * @param key   the key
     * @param value the long value
     * @return this builder
     */
    public RNbtCompoundBuilder putLong(@NotNull String key, long value) {
        return put(key, RNbtPrimitive.ofLong(value));
    }

    /**
     * Adds a float entry.
     *
     * @param key   the key
     * @param value the float value
     * @return this builder
     */
    public RNbtCompoundBuilder putFloat(@NotNull String key, float value) {
        return put(key, RNbtPrimitive.ofFloat(value));
    }

    /**
     * Adds a double entry.
     *
     * @param key   the key
     * @param value the double value
     * @return this builder
     */
    public RNbtCompoundBuilder putDouble(@NotNull String key, double value) {
        return put(key, RNbtPrimitive.ofDouble(value));
    }

    /**
     * Adds a boolean entry (stored as a BYTE).
     *
     * @param key   the key
     * @param value the boolean value
     * @return this builder
     */
    public RNbtCompoundBuilder putBoolean(@NotNull String key, boolean value) {
        return put(key, RNbtPrimitive.ofBoolean(value));
    }

    /**
     * Adds a byte array entry.
     *
     * @param key   the key
     * @param value the byte array
     * @return this builder
     */
    public RNbtCompoundBuilder putByteArray(@NotNull String key, byte[] value) {
        return put(key, new RNbtByteArray(value));
    }

    /**
     * Adds an int array entry.
     *
     * @param key   the key
     * @param value the int array
     * @return this builder
     */
    public RNbtCompoundBuilder putIntArray(@NotNull String key, int[] value) {
        return put(key, new RNbtIntArray(value));
    }

    /**
     * Adds a long array entry.
     *
     * @param key   the key
     * @param value the long array
     * @return this builder
     */
    public RNbtCompoundBuilder putLongArray(@NotNull String key, long[] value) {
        return put(key, new RNbtLongArray(value));
    }

    /**
     * Removes an entry by key.
     *
     * @param key the key to remove
     * @return this builder
     */
    public RNbtCompoundBuilder remove(@NotNull String key) {
        entries.remove(Objects.requireNonNull(key, "key"));
        return this;
    }

    /**
     * Applies a typed field write to the builder.
     *
     * @param <T>   the value type
     * @param field the field descriptor
     * @param value the value
     * @return this builder
     */
    public <T> RNbtCompoundBuilder put(@NotNull RNbtField<T> field, @NotNull T value) {
        RNbtCompound updated = Objects.requireNonNull(field, "field").write(build(), value);
        entries.clear();
        entries.putAll(updated.asMap());
        return this;
    }

    /**
     * Replaces all entries with the entries from the given compound.
     *
     * @param compound the compound to copy entries from
     * @return this builder
     */
    public RNbtCompoundBuilder put(@NotNull RNbtCompound compound) {
        entries.clear();
        entries.putAll(Objects.requireNonNull(compound, "compound").asMap());
        return this;
    }

    /**
     * Builds an immutable {@link RNbtCompound} from the current builder state.
     *
     * @return a new RNbtCompound
     */
    public @NotNull RNbtCompound build() {
        return RNbtCompound.of(entries);
    }
}
