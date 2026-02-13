package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RNbtCompoundBuilder {
    private final LinkedHashMap<String, RNbtValue> entries = new LinkedHashMap<>();

    public RNbtCompoundBuilder put(@NotNull String key, @NotNull RNbtValue value) {
        entries.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        return this;
    }

    public RNbtCompoundBuilder putAll(@NotNull Map<String, ? extends RNbtValue> values) {
        Objects.requireNonNull(values, "values").forEach(this::put);
        return this;
    }

    public RNbtCompoundBuilder putString(@NotNull String key, @NotNull String value) {
        return put(key, RNbtPrimitive.ofString(value));
    }

    public RNbtCompoundBuilder putByte(@NotNull String key, byte value) {
        return put(key, RNbtPrimitive.ofByte(value));
    }

    public RNbtCompoundBuilder putShort(@NotNull String key, short value) {
        return put(key, RNbtPrimitive.ofShort(value));
    }

    public RNbtCompoundBuilder putInt(@NotNull String key, int value) {
        return put(key, RNbtPrimitive.ofInt(value));
    }

    public RNbtCompoundBuilder putLong(@NotNull String key, long value) {
        return put(key, RNbtPrimitive.ofLong(value));
    }

    public RNbtCompoundBuilder putFloat(@NotNull String key, float value) {
        return put(key, RNbtPrimitive.ofFloat(value));
    }

    public RNbtCompoundBuilder putDouble(@NotNull String key, double value) {
        return put(key, RNbtPrimitive.ofDouble(value));
    }

    public RNbtCompoundBuilder putBoolean(@NotNull String key, boolean value) {
        return put(key, RNbtPrimitive.ofBoolean(value));
    }

    public RNbtCompoundBuilder putByteArray(@NotNull String key, byte[] value) {
        return put(key, new RNbtByteArray(value));
    }

    public RNbtCompoundBuilder putIntArray(@NotNull String key, int[] value) {
        return put(key, new RNbtIntArray(value));
    }

    public RNbtCompoundBuilder putLongArray(@NotNull String key, long[] value) {
        return put(key, new RNbtLongArray(value));
    }

    public RNbtCompoundBuilder remove(@NotNull String key) {
        entries.remove(Objects.requireNonNull(key, "key"));
        return this;
    }

    public <T> RNbtCompoundBuilder put(@NotNull RNbtField<T> field, @NotNull T value) {
        RNbtCompound updated = Objects.requireNonNull(field, "field").write(build(), value);
        entries.clear();
        entries.putAll(updated.asMap());
        return this;
    }

    public RNbtCompoundBuilder put(@NotNull RNbtCompound compound) {
        entries.clear();
        entries.putAll(Objects.requireNonNull(compound, "compound").asMap());
        return this;
    }

    public @NotNull RNbtCompound build() {
        return RNbtCompound.of(entries);
    }
}
