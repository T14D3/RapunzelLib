package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An immutable NBT compound value - a map of string keys to {@link RNbtValue} entries.
 * <p>
 * Compounds are the primary container type in NBT, analogous to a JSON object.
 * Operations produce new immutable copies rather than mutating in place.</p>
 */
public final class RNbtCompound implements RNbtValue {
    private static final RNbtCompound EMPTY = new RNbtCompound(Map.of(), true);

    private final @NotNull Map<String, RNbtValue> entries;

    public RNbtCompound(@NotNull Map<String, ? extends RNbtValue> entries) {
        this(entries, false);
    }

    private RNbtCompound(@NotNull Map<String, ? extends RNbtValue> entries, boolean trusted) {
        Objects.requireNonNull(entries, "entries");
        if (trusted) {
            this.entries = Collections.unmodifiableMap((Map<String, RNbtValue>) entries);
            return;
        }
        LinkedHashMap<String, RNbtValue> copy = new LinkedHashMap<>();
        entries.forEach((key, value) -> copy.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, () -> "value for key '" + key + "'")));
        this.entries = Collections.unmodifiableMap(copy);
    }

    /**
     * Returns the empty compound singleton.
     *
     * @return the empty compound
     */
    public static @NotNull RNbtCompound empty() {
        return EMPTY;
    }

    /**
     * Creates a compound from the given map of entries. Returns the empty singleton if the map is empty.
     *
     * @param entries the entry map
     * @return a new RNbtCompound
     */
    public static @NotNull RNbtCompound of(@NotNull Map<String, ? extends RNbtValue> entries) {
        return entries.isEmpty() ? EMPTY : new RNbtCompound(entries);
    }

    /**
     * Creates a new {@link RNbtCompoundBuilder} for building compounds fluently.
     *
     * @return a new builder
     */
    public static @NotNull RNbtCompoundBuilder builder() {
        return new RNbtCompoundBuilder();
    }

    @Override
    public @NotNull RNbtType type() {
        return RNbtType.COMPOUND;
    }

    /**
     * Whether this compound contains no entries.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Whether this compound contains the given key.
     *
     * @param key the key to check
     * @return true if the key exists
     */
    public boolean contains(@NotNull String key) {
        return entries.containsKey(Objects.requireNonNull(key, "key"));
    }

    /**
     * Looks up a value by key.
     *
     * @param key the NBT key to look up
     * @return an Optional containing the value, or empty if absent
     */
    public @NotNull Optional<RNbtValue> get(@NotNull String key) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(key, "key")));
    }

    /**
     * Returns the underlying map of entries (unmodifiable).
     *
     * @return the entry map
     */
    public @NotNull Map<String, RNbtValue> asMap() {
        return entries;
    }

    /**
     * Returns the set of keys in this compound.
     *
     * @return the key set
     */
    public @NotNull Set<String> keys() {
        return entries.keySet();
    }

    /**
     * Returns a new compound with the given key-value pair added (or replaced if the key already exists).
     *
     * @param key   the key
     * @param value the value
     * @return a new compound with the entry
     */
    public @NotNull RNbtCompound put(@NotNull String key, @NotNull RNbtValue value) {
        LinkedHashMap<String, RNbtValue> copy = new LinkedHashMap<>(entries);
        copy.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        return new RNbtCompound(copy, true);
    }

    /**
     * Returns a new compound with the given key removed (or this compound if absent).
     *
     * @param key the key to remove
     * @return a new compound without the entry
     */
    public @NotNull RNbtCompound remove(@NotNull String key) {
        if (!entries.containsKey(Objects.requireNonNull(key, "key"))) {
            return this;
        }
        LinkedHashMap<String, RNbtValue> copy = new LinkedHashMap<>(entries);
        copy.remove(key);
        return copy.isEmpty() ? EMPTY : new RNbtCompound(copy, true);
    }

    /**
     * Reads a value from this compound using a typed field.
     *
     * @param <T>   the value type
     * @param field the field descriptor
     * @return an Optional containing the decoded value, or empty if absent
     */
    public <T> @NotNull Optional<T> get(@NotNull RNbtField<T> field) {
        return Objects.requireNonNull(field, "field").read(this);
    }

    /**
     * Reads a value from this compound using a typed path.
     *
     * @param <T>  the value type
     * @param path the path descriptor
     * @return an Optional containing the decoded value, or empty if absent
     */
    public <T> @NotNull Optional<T> get(@NotNull RNbtPath<T> path) {
        return Objects.requireNonNull(path, "path").read(this);
    }

    /**
     * Writes a typed field value into this compound, returning the new compound.
     *
     * @param <T>   the value type
     * @param field the field descriptor
     * @param value the value to write
     * @return a new compound with the field applied
     */
    public <T> @NotNull RNbtCompound put(@NotNull RNbtField<T> field, @NotNull T value) {
        return Objects.requireNonNull(field, "field").write(this, value);
    }

    /**
     * Writes a typed path value into this compound, returning the new compound.
     *
     * @param <T>   the value type
     * @param path  the path descriptor
     * @param value the value to write
     * @return a new compound with the path applied
     */
    public <T> @NotNull RNbtCompound put(@NotNull RNbtPath<T> path, @NotNull T value) {
        return Objects.requireNonNull(path, "path").write(this, value);
    }

    /**
     * Removes the value at the given path, returning the new compound.
     *
     * @param path the path to remove
     * @return a new compound with the path removed
     */
    public @NotNull RNbtCompound remove(@NotNull RNbtPath<?> path) {
        return Objects.requireNonNull(path, "path").remove(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RNbtCompound that)) {
            return false;
        }
        return entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public String toString() {
        return "RNbtCompound" + entries;
    }
}
