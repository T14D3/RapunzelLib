package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An immutable NBT primitive value wrapping a single scalar (string or numeric) value.
 * <p>
 * Supported types: STRING, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, and BOOLEAN (stored as BYTE).</p>
 */
public final class RNbtPrimitive implements RNbtValue {
    private final @NotNull RNbtType type;
    private final @NotNull Object value;

    private RNbtPrimitive(@NotNull RNbtType type, @NotNull Object value) {
        this.type = validateType(type);
        this.value = Objects.requireNonNull(value, "value");
    }

    /**
     * Creates a primitive STRING value.
     *
     * @param value the string value
     * @return a new RNbtPrimitive
     */
    public static @NotNull RNbtPrimitive ofString(@NotNull String value) {
        return new RNbtPrimitive(RNbtType.STRING, value);
    }

    /**
     * Creates a primitive BYTE value.
     *
     * @param value the byte value
     * @return a new RNbtPrimitive
     */
    public static @NotNull RNbtPrimitive ofByte(byte value) {
        return new RNbtPrimitive(RNbtType.BYTE, Byte.valueOf(value));
    }

    /**
     * Creates a primitive SHORT value.
     *
     * @param value the short value
     * @return a new RNbtPrimitive
     */
    public static @NotNull RNbtPrimitive ofShort(short value) {
        return new RNbtPrimitive(RNbtType.SHORT, Short.valueOf(value));
    }

    /**
     * Creates a primitive INT value.
     *
     * @param value the int value
     * @return a new RNbtPrimitive
     */
    public static @NotNull RNbtPrimitive ofInt(int value) {
        return new RNbtPrimitive(RNbtType.INT, Integer.valueOf(value));
    }

    /**
     * Creates a primitive LONG value.
     *
     * @param value the long value
     * @return a new RNbtPrimitive
     */
    public static @NotNull RNbtPrimitive ofLong(long value) {
        return new RNbtPrimitive(RNbtType.LONG, Long.valueOf(value));
    }

    /**
     * Creates a primitive FLOAT value.
     *
     * @param value the float value
     * @return a new RNbtPrimitive
     */
    public static @NotNull RNbtPrimitive ofFloat(float value) {
        return new RNbtPrimitive(RNbtType.FLOAT, Float.valueOf(value));
    }

    /**
     * Creates a primitive DOUBLE value.
     *
     * @param value the double value
     * @return a new RNbtPrimitive
     */
    public static @NotNull RNbtPrimitive ofDouble(double value) {
        return new RNbtPrimitive(RNbtType.DOUBLE, Double.valueOf(value));
    }

    /**
     * Creates a primitive BOOLEAN value (stored internally as BYTE 0 or 1).
     *
     * @param value the boolean value
     * @return a new RNbtPrimitive
     */
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

    /**
     * Returns the raw wrapped value.
     *
     * @return the raw Object value
     */
    public @NotNull Object value() {
        return value;
    }

    /**
     * Returns the string value.
     *
     * @return the string value
     * @throws IllegalStateException if this value is not of type STRING
     */
    public @NotNull String stringValue() {
        if (type != RNbtType.STRING) {
            throw new IllegalStateException("Expected STRING value but got " + type);
        }
        return (String) value;
    }

    /**
     * Returns the numeric value as a {@link Number}.
     *
     * @return the numeric value
     * @throws IllegalStateException if this value is not numeric
     */
    public @NotNull Number numberValue() {
        if (!type.isNumeric()) {
            throw new IllegalStateException("Expected numeric NBT value but got " + type);
        }
        return (Number) value;
    }

    /**
     * Returns the byte value.
     *
     * @return the byte value
     */
    public byte byteValue() {
        return numberValue().byteValue();
    }

    /**
     * Returns the short value.
     *
     * @return the short value
     */
    public short shortValue() {
        return numberValue().shortValue();
    }

    /**
     * Returns the int value.
     *
     * @return the int value
     */
    public int intValue() {
        return numberValue().intValue();
    }

    /**
     * Returns the long value.
     *
     * @return the long value
     */
    public long longValue() {
        return numberValue().longValue();
    }

    /**
     * Returns the float value.
     *
     * @return the float value
     */
    public float floatValue() {
        return numberValue().floatValue();
    }

    /**
     * Returns the double value.
     *
     * @return the double value
     */
    public double doubleValue() {
        return numberValue().doubleValue();
    }

    /**
     * Returns the boolean value (true if the byte value is non-zero).
     *
     * @return the boolean value
     */
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
