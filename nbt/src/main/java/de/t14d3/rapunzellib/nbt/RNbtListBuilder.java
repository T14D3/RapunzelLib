package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A mutable builder for constructing {@link RNbtList} instances fluently.
 * <p>
 * Provides convenience methods for adding primitive-typed elements.</p>
 */
public final class RNbtListBuilder {
    private RNbtType elementType = RNbtType.END;
    private final ArrayList<RNbtValue> values = new ArrayList<>();

    /**
     * Creates a builder with no declared element type (inferred from the first added element).
     */
    public RNbtListBuilder() {
    }

    /**
     * Creates a builder with the given element type.
     *
     * @param elementType the required element type
     */
    public RNbtListBuilder(@NotNull RNbtType elementType) {
        this.elementType = Objects.requireNonNull(elementType, "elementType");
    }

    /**
     * Adds an NBT value to the list.
     *
     * @param value the value to add
     * @return this builder
     * @throws IllegalArgumentException if the element type does not match the declared type
     */
    public RNbtListBuilder add(@NotNull RNbtValue value) {
        RNbtValue nbtValue = Objects.requireNonNull(value, "value");
        if (elementType == RNbtType.END) {
            elementType = nbtValue.type();
        } else if (nbtValue.type() != elementType) {
            throw new IllegalArgumentException("List expects " + elementType + " elements but got " + nbtValue.type());
        }
        values.add(nbtValue);
        return this;
    }

    /**
     * Adds a string value.
     *
     * @param value the string value
     * @return this builder
     */
    public RNbtListBuilder addString(@NotNull String value) {
        return add(RNbtPrimitive.ofString(value));
    }

    /**
     * Adds a byte value.
     *
     * @param value the byte value
     * @return this builder
     */
    public RNbtListBuilder addByte(byte value) {
        return add(RNbtPrimitive.ofByte(value));
    }

    /**
     * Adds a short value.
     *
     * @param value the short value
     * @return this builder
     */
    public RNbtListBuilder addShort(short value) {
        return add(RNbtPrimitive.ofShort(value));
    }

    /**
     * Adds an int value.
     *
     * @param value the int value
     * @return this builder
     */
    public RNbtListBuilder addInt(int value) {
        return add(RNbtPrimitive.ofInt(value));
    }

    /**
     * Adds a long value.
     *
     * @param value the long value
     * @return this builder
     */
    public RNbtListBuilder addLong(long value) {
        return add(RNbtPrimitive.ofLong(value));
    }

    /**
     * Adds a float value.
     *
     * @param value the float value
     * @return this builder
     */
    public RNbtListBuilder addFloat(float value) {
        return add(RNbtPrimitive.ofFloat(value));
    }

    /**
     * Adds a double value.
     *
     * @param value the double value
     * @return this builder
     */
    public RNbtListBuilder addDouble(double value) {
        return add(RNbtPrimitive.ofDouble(value));
    }

    /**
     * Adds a boolean value (stored as a BYTE).
     *
     * @param value the boolean value
     * @return this builder
     */
    public RNbtListBuilder addBoolean(boolean value) {
        return add(RNbtPrimitive.ofBoolean(value));
    }

    /**
     * Adds all values from the given collection.
     *
     * @param values the values to add
     * @return this builder
     */
    public RNbtListBuilder addAll(@NotNull List<? extends RNbtValue> values) {
        Objects.requireNonNull(values, "values").forEach(this::add);
        return this;
    }

    /**
     * Builds an immutable {@link RNbtList} from the current builder state.
     *
     * @return a new RNbtList
     */
    public @NotNull RNbtList build() {
        return values.isEmpty() ? RNbtList.of(elementType, List.of()) : RNbtList.of(elementType, values);
    }
}
