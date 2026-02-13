package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public final class RNbtLongArray implements RNbtValue {
    private final long @NotNull [] value;

    public RNbtLongArray(long @NotNull [] value) {
        this.value = Objects.requireNonNull(value, "value").clone();
    }

    @Override
    public @NotNull RNbtType type() {
        return RNbtType.LONG_ARRAY;
    }

    public long @NotNull [] value() {
        return value.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RNbtLongArray that)) {
            return false;
        }
        return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return "RNbtLongArray[length=" + value.length + ']';
    }
}
