package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

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

    public @NotNull Class<?> valueClass() {
        return valueClass;
    }

    public boolean isNumeric() {
        return this == BYTE || this == SHORT || this == INT || this == LONG || this == FLOAT || this == DOUBLE;
    }

    public boolean isArray() {
        return this == BYTE_ARRAY || this == INT_ARRAY || this == LONG_ARRAY;
    }

    public boolean isContainer() {
        return this == LIST || this == COMPOUND;
    }
}
