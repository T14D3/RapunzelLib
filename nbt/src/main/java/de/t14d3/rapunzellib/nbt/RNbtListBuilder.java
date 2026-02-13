package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RNbtListBuilder {
    private RNbtType elementType = RNbtType.END;
    private final ArrayList<RNbtValue> values = new ArrayList<>();

    public RNbtListBuilder() {
    }

    public RNbtListBuilder(@NotNull RNbtType elementType) {
        this.elementType = Objects.requireNonNull(elementType, "elementType");
    }

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

    public RNbtListBuilder addString(@NotNull String value) {
        return add(RNbtPrimitive.ofString(value));
    }

    public RNbtListBuilder addByte(byte value) {
        return add(RNbtPrimitive.ofByte(value));
    }

    public RNbtListBuilder addShort(short value) {
        return add(RNbtPrimitive.ofShort(value));
    }

    public RNbtListBuilder addInt(int value) {
        return add(RNbtPrimitive.ofInt(value));
    }

    public RNbtListBuilder addLong(long value) {
        return add(RNbtPrimitive.ofLong(value));
    }

    public RNbtListBuilder addFloat(float value) {
        return add(RNbtPrimitive.ofFloat(value));
    }

    public RNbtListBuilder addDouble(double value) {
        return add(RNbtPrimitive.ofDouble(value));
    }

    public RNbtListBuilder addBoolean(boolean value) {
        return add(RNbtPrimitive.ofBoolean(value));
    }

    public RNbtListBuilder addAll(@NotNull List<? extends RNbtValue> values) {
        Objects.requireNonNull(values, "values").forEach(this::add);
        return this;
    }

    public @NotNull RNbtList build() {
        return values.isEmpty() ? RNbtList.of(elementType, List.of()) : RNbtList.of(elementType, values);
    }
}
