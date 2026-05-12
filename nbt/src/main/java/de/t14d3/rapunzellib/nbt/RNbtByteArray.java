package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable NBT byte array value wrapping a {@code byte[]}.
 */
public final class RNbtByteArray implements RNbtValue {
    private final byte @NotNull [] value;

    /**
     * Creates a byte array NBT value from a defensive copy of the given array.
     *
     * @param value the byte array (will be cloned)
     */
    public RNbtByteArray(byte @NotNull [] value) {
        this.value = Objects.requireNonNull(value, "value").clone();
    }

    @Override
    public @NotNull RNbtType type() {
        return RNbtType.BYTE_ARRAY;
    }

    /**
     * Returns a defensive copy of the underlying byte array.
     *
     * @return the byte array
     */
    public byte @NotNull [] value() {
        return value.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RNbtByteArray that)) {
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
        return "RNbtByteArray[length=" + value.length + ']';
    }
}
