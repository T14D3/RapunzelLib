package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

    public static @NotNull RNbtCompound empty() {
        return EMPTY;
    }

    public static @NotNull RNbtCompound of(@NotNull Map<String, ? extends RNbtValue> entries) {
        return entries.isEmpty() ? EMPTY : new RNbtCompound(entries);
    }

    public static @NotNull RNbtCompoundBuilder builder() {
        return new RNbtCompoundBuilder();
    }

    @Override
    public @NotNull RNbtType type() {
        return RNbtType.COMPOUND;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean contains(@NotNull String key) {
        return entries.containsKey(Objects.requireNonNull(key, "key"));
    }

    public @NotNull Optional<RNbtValue> get(@NotNull String key) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(key, "key")));
    }

    public @NotNull Map<String, RNbtValue> asMap() {
        return entries;
    }

    public @NotNull Set<String> keys() {
        return entries.keySet();
    }

    public @NotNull RNbtCompound put(@NotNull String key, @NotNull RNbtValue value) {
        LinkedHashMap<String, RNbtValue> copy = new LinkedHashMap<>(entries);
        copy.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        return new RNbtCompound(copy, true);
    }

    public @NotNull RNbtCompound remove(@NotNull String key) {
        if (!entries.containsKey(Objects.requireNonNull(key, "key"))) {
            return this;
        }
        LinkedHashMap<String, RNbtValue> copy = new LinkedHashMap<>(entries);
        copy.remove(key);
        return copy.isEmpty() ? EMPTY : new RNbtCompound(copy, true);
    }

    public <T> @NotNull Optional<T> get(@NotNull RNbtField<T> field) {
        return Objects.requireNonNull(field, "field").read(this);
    }

    public <T> @NotNull Optional<T> get(@NotNull RNbtPath<T> path) {
        return Objects.requireNonNull(path, "path").read(this);
    }

    public <T> @NotNull RNbtCompound put(@NotNull RNbtField<T> field, @NotNull T value) {
        return Objects.requireNonNull(field, "field").write(this, value);
    }

    public <T> @NotNull RNbtCompound put(@NotNull RNbtPath<T> path, @NotNull T value) {
        return Objects.requireNonNull(path, "path").write(this, value);
    }

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
