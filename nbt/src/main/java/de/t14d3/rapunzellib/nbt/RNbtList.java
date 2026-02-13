package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RNbtList implements RNbtValue, Iterable<RNbtValue> {
    private static final RNbtList EMPTY = new RNbtList(RNbtType.END, List.of(), true);

    private final @NotNull RNbtType elementType;
    private final @NotNull List<RNbtValue> values;

    public RNbtList(@NotNull RNbtType elementType, @NotNull List<? extends RNbtValue> values) {
        this(elementType, values, false);
    }

    private RNbtList(@NotNull RNbtType elementType, @NotNull List<? extends RNbtValue> values, boolean trusted) {
        this.elementType = Objects.requireNonNull(elementType, "elementType");
        Objects.requireNonNull(values, "values");
        if (trusted) {
            this.values = Collections.unmodifiableList((List<RNbtValue>) values);
            return;
        }
        ArrayList<RNbtValue> copy = new ArrayList<>(values.size());
        for (RNbtValue value : values) {
            RNbtValue nbtValue = Objects.requireNonNull(value, "value");
            validateElementType(elementType, nbtValue.type());
            copy.add(nbtValue);
        }
        this.values = Collections.unmodifiableList(copy);
    }

    public static @NotNull RNbtList empty() {
        return EMPTY;
    }

    public static @NotNull RNbtList of(@NotNull List<? extends RNbtValue> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            return EMPTY;
        }
        RNbtType elementType = values.getFirst().type();
        return new RNbtList(elementType, values);
    }

    public static @NotNull RNbtList of(@NotNull RNbtType elementType, @NotNull List<? extends RNbtValue> values) {
        return values.isEmpty() ? new RNbtList(elementType, List.of()) : new RNbtList(elementType, values);
    }

    public static @NotNull RNbtListBuilder builder() {
        return new RNbtListBuilder();
    }

    public static @NotNull RNbtListBuilder builder(@NotNull RNbtType elementType) {
        return new RNbtListBuilder(elementType);
    }

    private static void validateElementType(@NotNull RNbtType expectedType, @NotNull RNbtType actualType) {
        if (expectedType != RNbtType.END && expectedType != actualType) {
            throw new IllegalArgumentException("List expects " + expectedType + " elements but got " + actualType);
        }
    }

    @Override
    public @NotNull RNbtType type() {
        return RNbtType.LIST;
    }

    public @NotNull RNbtType elementType() {
        return elementType;
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public @NotNull List<RNbtValue> values() {
        return values;
    }

    public @NotNull Optional<RNbtValue> get(int index) {
        return index < 0 || index >= values.size() ? Optional.empty() : Optional.of(values.get(index));
    }

    public @NotNull RNbtValue getOrThrow(int index) {
        return values.get(index);
    }

    public @NotNull RNbtList add(@NotNull RNbtValue value) {
        RNbtValue nbtValue = Objects.requireNonNull(value, "value");
        RNbtType newElementType = elementType == RNbtType.END ? nbtValue.type() : elementType;
        validateElementType(newElementType, nbtValue.type());
        ArrayList<RNbtValue> copy = new ArrayList<>(values);
        copy.add(nbtValue);
        return new RNbtList(newElementType, copy, true);
    }

    public @NotNull RNbtList set(int index, @NotNull RNbtValue value) {
        RNbtValue nbtValue = Objects.requireNonNull(value, "value");
        if (index < 0 || index >= values.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for list of size " + values.size());
        }
        RNbtType newElementType = elementType == RNbtType.END ? nbtValue.type() : elementType;
        validateElementType(newElementType, nbtValue.type());
        ArrayList<RNbtValue> copy = new ArrayList<>(values);
        copy.set(index, nbtValue);
        return new RNbtList(newElementType, copy, true);
    }

    public @NotNull RNbtList remove(int index) {
        if (index < 0 || index >= values.size()) {
            return this;
        }
        ArrayList<RNbtValue> copy = new ArrayList<>(values);
        copy.remove(index);
        if (copy.isEmpty()) {
            return elementType == RNbtType.END ? EMPTY : new RNbtList(elementType, List.of());
        }
        return new RNbtList(elementType, copy, true);
    }

    @Override
    public @NotNull Iterator<RNbtValue> iterator() {
        return values.iterator();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RNbtList that)) {
            return false;
        }
        return elementType == that.elementType && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType, values);
    }

    @Override
    public String toString() {
        return "RNbtList[elementType=" + elementType + ", values=" + values + ']';
    }
}
