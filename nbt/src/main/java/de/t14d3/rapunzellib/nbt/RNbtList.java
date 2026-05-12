package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable NBT list value - an ordered list of homogeneous {@link RNbtValue} elements.
 * <p>
 * All elements must have the same {@link RNbtType}. An empty list may carry type {@link RNbtType#END}
 * until an element is added.</p>
 */
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

    /**
     * Returns the empty list singleton.
     *
     * @return the empty list
     */
    public static @NotNull RNbtList empty() {
        return EMPTY;
    }

    /**
     * Creates a list from the given values, inferring the element type from the first element.
     *
     * @param values the list values
     * @return a new RNbtList
     */
    public static @NotNull RNbtList of(@NotNull List<? extends RNbtValue> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            return EMPTY;
        }
        RNbtType elementType = values.getFirst().type();
        return new RNbtList(elementType, values);
    }

    /**
     * Creates a list with the given explicit element type and values.
     *
     * @param elementType the element type
     * @param values      the list values
     * @return a new RNbtList
     */
    public static @NotNull RNbtList of(@NotNull RNbtType elementType, @NotNull List<? extends RNbtValue> values) {
        return values.isEmpty() ? new RNbtList(elementType, List.of()) : new RNbtList(elementType, values);
    }

    /**
     * Creates a new builder with inferred element type.
     *
     * @return a new builder
     */
    public static @NotNull RNbtListBuilder builder() {
        return new RNbtListBuilder();
    }

    /**
     * Creates a new builder with the given element type.
     *
     * @param elementType the element type
     * @return a new builder
     */
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

    /**
     * Returns the type of elements in this list.
     *
     * @return the element type
     */
    public @NotNull RNbtType elementType() {
        return elementType;
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the size
     */
    public int size() {
        return values.size();
    }

    /**
     * Whether this list is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Returns the underlying list of values (unmodifiable).
     *
     * @return the values list
     */
    public @NotNull List<RNbtValue> values() {
        return values;
    }

    /**
     * Gets the element at the given index, if it exists.
     *
     * @param index the index
     * @return an Optional containing the element, or empty if out of bounds
     */
    public @NotNull Optional<RNbtValue> get(int index) {
        return index < 0 || index >= values.size() ? Optional.empty() : Optional.of(values.get(index));
    }

    /**
     * Gets the element at the given index, throwing if out of bounds.
     *
     * @param index the index
     * @return the element
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public @NotNull RNbtValue getOrThrow(int index) {
        return values.get(index);
    }

    /**
     * Returns a new list with the value appended.
     *
     * @param value the value to add
     * @return a new RNbtList
     */
    public @NotNull RNbtList add(@NotNull RNbtValue value) {
        RNbtValue nbtValue = Objects.requireNonNull(value, "value");
        RNbtType newElementType = elementType == RNbtType.END ? nbtValue.type() : elementType;
        validateElementType(newElementType, nbtValue.type());
        ArrayList<RNbtValue> copy = new ArrayList<>(values);
        copy.add(nbtValue);
        return new RNbtList(newElementType, copy, true);
    }

    /**
     * Returns a new list with the element at the given index replaced.
     *
     * @param index the index
     * @param value the new value
     * @return a new RNbtList
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
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

    /**
     * Returns a new list with the element at the given index removed.
     *
     * @param index the index
     * @return a new RNbtList (or this list if the index is out of bounds)
     */
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
