package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public sealed interface RNbtValue extends Serializable permits RNbtPrimitive, RNbtCompound, RNbtList, RNbtByteArray, RNbtIntArray, RNbtLongArray {

    @NotNull RNbtType type();

    default @NotNull RNbtPrimitive asPrimitive() {
        if (this instanceof RNbtPrimitive primitive) {
            return primitive;
        }
        throw new IllegalStateException("Expected primitive NBT value but got " + type());
    }

    default @NotNull RNbtCompound asCompound() {
        if (this instanceof RNbtCompound compound) {
            return compound;
        }
        throw new IllegalStateException("Expected compound NBT value but got " + type());
    }

    default @NotNull RNbtList asList() {
        if (this instanceof RNbtList list) {
            return list;
        }
        throw new IllegalStateException("Expected list NBT value but got " + type());
    }

    static @NotNull RNbtPrimitive string(@NotNull String value) {
        return RNbtPrimitive.ofString(value);
    }

    static @NotNull RNbtPrimitive byteValue(byte value) {
        return RNbtPrimitive.ofByte(value);
    }

    static @NotNull RNbtPrimitive shortValue(short value) {
        return RNbtPrimitive.ofShort(value);
    }

    static @NotNull RNbtPrimitive intValue(int value) {
        return RNbtPrimitive.ofInt(value);
    }

    static @NotNull RNbtPrimitive longValue(long value) {
        return RNbtPrimitive.ofLong(value);
    }

    static @NotNull RNbtPrimitive floatValue(float value) {
        return RNbtPrimitive.ofFloat(value);
    }

    static @NotNull RNbtPrimitive doubleValue(double value) {
        return RNbtPrimitive.ofDouble(value);
    }

    static @NotNull RNbtPrimitive bool(boolean value) {
        return RNbtPrimitive.ofBoolean(value);
    }

    static @NotNull RNbtByteArray byteArray(byte[] value) {
        return new RNbtByteArray(value);
    }

    static @NotNull RNbtIntArray intArray(int[] value) {
        return new RNbtIntArray(value);
    }

    static @NotNull RNbtLongArray longArray(long[] value) {
        return new RNbtLongArray(value);
    }
}
