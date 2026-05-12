package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

/**
 * Enum representing all standard NBT tag types as defined by the Minecraft protocol.
 * Each constant maps to a Java class used to hold the value.
 */
public enum RNbtType {
    END(Void.class),
    BYTE(Byte.class),
    SHORT(Short.class),
    INT(Integer.class),
    LONG(Long.class),
    FLOAT(Float.class),
    DOUBLE(Double.class),
    BYTE_ARRAY(byte[].class),
    STRING(String.class),
    LIST(RNbtList.class),
    COMPOUND(RNbtCompound.class),
    INT_ARRAY(int[].class),
    LONG_ARRAY(long[].class);

    private final @NotNull Class<?> valueClass;

    RNbtType(@NotNull Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    /**
     * Returns the Java class associated with this NBT type.
     *
     * @return the corresponding value class
     */
    public @NotNull Class<?> valueClass() {
        return valueClass;
    }

    /**
     * Whether this type represents a numeric primitive (BYTE, SHORT, INT, LONG, FLOAT, DOUBLE).
     *
     * @return true if numeric
     */
    public boolean isNumeric() {
        return this == BYTE || this == SHORT || this == INT || this == LONG || this == FLOAT || this == DOUBLE;
    }

    /**
     * Whether this type is an array type (BYTE_ARRAY, INT_ARRAY, LONG_ARRAY).
     *
     * @return true if an array type
     */
    public boolean isArray() {
        return this == BYTE_ARRAY || this == INT_ARRAY || this == LONG_ARRAY;
    }

    /**
     * Whether this type is a container type (LIST or COMPOUND) that can hold child values.
     *
     * @return true if a container type
     */
    public boolean isContainer() {
        return this == LIST || this == COMPOUND;
    }
}
