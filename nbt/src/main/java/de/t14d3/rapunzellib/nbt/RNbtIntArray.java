package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public final class RNbtIntArray implements RNbtValue {
    private final int @NotNull [] value;

    public RNbtIntArray(int @NotNull [] value) {
        this.value = Objects.requireNonNull(value, "value").clone();
    }

    @Override
    public @NotNull RNbtType type() {
        return RNbtType.INT_ARRAY;
    }

    public int @NotNull [] value() {
        return value.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RNbtIntArray that)) {
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
        return "RNbtIntArray[length=" + value.length + ']';
    }
}
