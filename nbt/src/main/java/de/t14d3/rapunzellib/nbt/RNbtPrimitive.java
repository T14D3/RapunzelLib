package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class RNbtPrimitive implements RNbtValue {
    private final @NotNull RNbtType type;
    private final @NotNull Object value;

    private RNbtPrimitive(@NotNull RNbtType type, @NotNull Object value) {
        this.type = validateType(type);
        this.value = Objects.requireNonNull(value, "value");
    }

    public static @NotNull RNbtPrimitive ofString(@NotNull String value) {
        return new RNbtPrimitive(RNbtType.STRING, value);
    }

    public static @NotNull RNbtPrimitive ofByte(byte value) {
        return new RNbtPrimitive(RNbtType.BYTE, Byte.valueOf(value));
    }

    public static @NotNull RNbtPrimitive ofShort(short value) {
        return new RNbtPrimitive(RNbtType.SHORT, Short.valueOf(value));
    }

    public static @NotNull RNbtPrimitive ofInt(int value) {
        return new RNbtPrimitive(RNbtType.INT, Integer.valueOf(value));
    }

    public static @NotNull RNbtPrimitive ofLong(long value) {
        return new RNbtPrimitive(RNbtType.LONG, Long.valueOf(value));
    }

    public static @NotNull RNbtPrimitive ofFloat(float value) {
        return new RNbtPrimitive(RNbtType.FLOAT, Float.valueOf(value));
    }

    public static @NotNull RNbtPrimitive ofDouble(double value) {
        return new RNbtPrimitive(RNbtType.DOUBLE, Double.valueOf(value));
    }

    public static @NotNull RNbtPrimitive ofBoolean(boolean value) {
        return ofByte((byte) (value ? 1 : 0));
    }

    private static @NotNull RNbtType validateType(@NotNull RNbtType type) {
        Objects.requireNonNull(type, "type");
        if (type == RNbtType.STRING || type.isNumeric()) {
            return type;
        }
        throw new IllegalArgumentException("Primitive NBT value cannot use type " + type);
    }

    @Override
    public @NotNull RNbtType type() {
        return type;
    }

    public @NotNull Object value() {
        return value;
    }

    public @NotNull String stringValue() {
        if (type != RNbtType.STRING) {
            throw new IllegalStateException("Expected STRING value but got " + type);
        }
        return (String) value;
    }

    public @NotNull Number numberValue() {
        if (!type.isNumeric()) {
            throw new IllegalStateException("Expected numeric NBT value but got " + type);
        }
        return (Number) value;
    }

    public byte byteValue() {
        return numberValue().byteValue();
    }

    public short shortValue() {
        return numberValue().shortValue();
    }

    public int intValue() {
        return numberValue().intValue();
    }

    public long longValue() {
        return numberValue().longValue();
    }

    public float floatValue() {
        return numberValue().floatValue();
    }

    public double doubleValue() {
        return numberValue().doubleValue();
    }

    public boolean booleanValue() {
        return byteValue() != 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RNbtPrimitive that)) {
            return false;
        }
        return type == that.type && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return "RNbtPrimitive[type=" + type + ", value=" + value + ']';
    }
}
