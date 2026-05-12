package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An immutable, simple implementation of {@link RItem} backed by plain fields.
 * <p>
 * All mutator methods throw {@link UnsupportedOperationException} as this type is designed
 * for use as an in-memory snapshot or transfer object.</p>
 */
final class SimpleRItem implements RItem {
    /** The item type reference. */
    private final @NotNull RRegistryRef<RItemType> typeRef;
    /** The stack amount. */
    private final int amount;
    /** The NBT data compound. */
    private final @NotNull RNbtCompound data;

    /**
     * Creates a simple item from a type reference, amount, and data.
     *
     * @param typeRef the type reference
     * @param amount  the stack amount
     * @param data    the NBT data compound
     */
    SimpleRItem(@NotNull RRegistryRef<RItemType> typeRef, int amount, @NotNull RNbtCompound data) {
        this.typeRef = Objects.requireNonNull(typeRef, "typeRef");
        this.amount = amount;
        this.data = Objects.requireNonNull(data, "data");
    }

    /**
     * Creates a simple item from a type key, amount, and data.
     *
     * @param typeKey the type key
     * @param amount  the stack amount
     * @param data    the NBT data compound
     */
    SimpleRItem(@NotNull RKey typeKey, int amount, @NotNull RNbtCompound data) {
        this(RItemType.ref(typeKey), amount, data);
    }

    /**
     * Creates a simple item from a builder.
     *
     * @param builder the item builder
     */
    SimpleRItem(@NotNull RItemBuilder builder) {
        this(builder.typeRef, builder.amount, builder.data);
    }

    @Override
    public @NotNull RRegistryRef<RItemType> typeRef() {
        return typeRef;
    }

    @Override
    public int amount() {
        return amount;
    }

    @Override
    public @NotNull RNbtCompound data() {
        return data;
    }

    /**
     * Returns a copy with the type key changed.
     *
     * @param newTypeKey the new type key
     * @return the new item
     */
    @Override
    public @NotNull RItem withTypeKey(@NotNull RKey newTypeKey) {
        return new SimpleRItem(newTypeKey, amount, data);
    }

    /**
     * Returns a copy with the amount changed.
     *
     * @param newAmount the new amount
     * @return the new item
     */
    @Override
    public @NotNull RItem withAmount(int newAmount) {
        return new SimpleRItem(typeRef, newAmount, data);
    }

    /**
     * Returns a copy with the NBT data replaced.
     *
     * @param data the new data compound
     * @return the new item
     */
    @Override
    public @NotNull RItem withData(@NotNull RNbtCompound data) {
        return new SimpleRItem(typeRef, amount, data);
    }

    /**
     * Always throws {@link UnsupportedOperationException} - SimpleRItem is immutable.
     */
    @Override
    public void setTypeKey(@NotNull RKey newTypeKey) {
        throw new UnsupportedOperationException("SimpleRItem is immutable");
    }

    /**
     * Always throws {@link UnsupportedOperationException} - SimpleRItem is immutable.
     */
    @Override
    public void setAmount(int newAmount) {
        throw new UnsupportedOperationException("SimpleRItem is immutable");
    }

    /**
     * Whether this stack is empty (amount <= 0).
     *
     * @return true if empty
     */
    @Override
    public boolean isEmpty() {
        return amount <= 0;
    }

    /**
     * Returns the count (same as amount for SimpleRItem).
     *
     * @return the count
     */
    @Override
    public int count() {
        return amount;
    }

    /**
     * Returns the max stack size (always 64 for SimpleRItem).
     *
     * @return 64
     */
    @Override
    public int maxStackSize() {
        return 64;
    }

    /**
     * Whether this item is similar to another (same type and data, ignoring amount).
     *
     * @param other the other item
     * @return true if similar
     */
    @Override
    public boolean isSimilar(@NotNull RItem other) {
        return typeKey().equals(other.typeKey()) && data.equals(other.data());
    }

    /**
     * Returns a copy with the count changed.
     *
     * @param count the new count
     * @return the new item
     */
    @Override
    public @NotNull RItem withCount(int count) {
        return withAmount(count);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RItem that)) {
            return false;
        }
        return amount == that.amount() && typeKey().equals(that.typeKey()) && data.equals(that.data());
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeKey(), amount, data);
    }

    @Override
    public String toString() {
        return "SimpleRItem[typeKey=" + typeKey() + ", amount=" + amount + ", data=" + data + ']';
    }
}
