package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Base sealed interface for all NBT value types in the Rapunzel NBT system.
 * <p>
 * Permitted implementations are all the standard NBT value wrappers: primitives, compounds,
 * lists, byte/int/long arrays. Every value has an associated {@link RNbtType}.</p>
 */
public sealed interface RNbtValue extends Serializable permits RNbtPrimitive, RNbtCompound, RNbtList, RNbtByteArray, RNbtIntArray, RNbtLongArray {

    /**
     * Returns the NBT type of this value.
     *
     * @return the NBT type
     */
    @NotNull RNbtType type();

    /**
     * Casts this value to a {@link RNbtPrimitive}.
     *
     * @return the primitive value
     * @throws IllegalStateException if this is not a primitive
     */
    default @NotNull RNbtPrimitive asPrimitive() {
        if (this instanceof RNbtPrimitive primitive) {
            return primitive;
        }
        throw new IllegalStateException("Expected primitive NBT value but got " + type());
    }

    /**
     * Casts this value to a {@link RNbtCompound}.
     *
     * @return the compound value
     * @throws IllegalStateException if this is not a compound
     */
    default @NotNull RNbtCompound asCompound() {
        if (this instanceof RNbtCompound compound) {
            return compound;
        }
        throw new IllegalStateException("Expected compound NBT value but got " + type());
    }

    /**
     * Casts this value to a {@link RNbtList}.
     *
     * @return the list value
     * @throws IllegalStateException if this is not a list
     */
    default @NotNull RNbtList asList() {
        if (this instanceof RNbtList list) {
            return list;
        }
        throw new IllegalStateException("Expected list NBT value but got " + type());
    }

    /**
     * Creates a primitive NBT string value.
     *
     * @param value the string value
     * @return a new RNbtPrimitive of type STRING
     */
    static @NotNull RNbtPrimitive string(@NotNull String value) {
        return RNbtPrimitive.ofString(value);
    }

    /**
     * Creates a primitive NBT byte value.
     *
     * @param value the byte value
     * @return a new RNbtPrimitive of type BYTE
     */
    static @NotNull RNbtPrimitive byteValue(byte value) {
        return RNbtPrimitive.ofByte(value);
    }

    /**
     * Creates a primitive NBT short value.
     *
     * @param value the short value
     * @return a new RNbtPrimitive of type SHORT
     */
    static @NotNull RNbtPrimitive shortValue(short value) {
        return RNbtPrimitive.ofShort(value);
    }

    /**
     * Creates a primitive NBT int value.
     *
     * @param value the int value
     * @return a new RNbtPrimitive of type INT
     */
    static @NotNull RNbtPrimitive intValue(int value) {
        return RNbtPrimitive.ofInt(value);
    }

    /**
     * Creates a primitive NBT long value.
     *
     * @param value the long value
     * @return a new RNbtPrimitive of type LONG
     */
    static @NotNull RNbtPrimitive longValue(long value) {
        return RNbtPrimitive.ofLong(value);
    }

    /**
     * Creates a primitive NBT float value.
     *
     * @param value the float value
     * @return a new RNbtPrimitive of type FLOAT
     */
    static @NotNull RNbtPrimitive floatValue(float value) {
        return RNbtPrimitive.ofFloat(value);
    }

    /**
     * Creates a primitive NBT double value.
     *
     * @param value the double value
     * @return a new RNbtPrimitive of type DOUBLE
     */
    static @NotNull RNbtPrimitive doubleValue(double value) {
        return RNbtPrimitive.ofDouble(value);
    }

    /**
     * Creates a primitive NBT boolean value (stored as a BYTE).
     *
     * @param value the boolean value
     * @return a new RNbtPrimitive of type BYTE
     */
    static @NotNull RNbtPrimitive bool(boolean value) {
        return RNbtPrimitive.ofBoolean(value);
    }

    /**
     * Creates an NBT byte array value.
     *
     * @param value the byte array
     * @return a new RNbtByteArray
     */
    static @NotNull RNbtByteArray byteArray(byte[] value) {
        return new RNbtByteArray(value);
    }

    /**
     * Creates an NBT int array value.
     *
     * @param value the int array
     * @return a new RNbtIntArray
     */
    static @NotNull RNbtIntArray intArray(int[] value) {
        return new RNbtIntArray(value);
    }

    /**
     * Creates an NBT long array value.
     *
     * @param value the long array
     * @return a new RNbtLongArray
     */
    static @NotNull RNbtLongArray longArray(long[] value) {
        return new RNbtLongArray(value);
    }
}
