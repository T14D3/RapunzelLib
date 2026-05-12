package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable NBT int array value wrapping an {@code int[]}.
 */
public final class RNbtIntArray implements RNbtValue {
    private final int @NotNull [] value;

    /**
     * Creates an int array NBT value from a defensive copy of the given array.
     *
     * @param value the int array (will be cloned)
     */
    public RNbtIntArray(int @NotNull [] value) {
        this.value = Objects.requireNonNull(value, "value").clone();
    }

    @Override
    public @NotNull RNbtType type() {
        return RNbtType.INT_ARRAY;
    }

    /**
     * Returns a defensive copy of the underlying int array.
     *
     * @return the int array
     */
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
