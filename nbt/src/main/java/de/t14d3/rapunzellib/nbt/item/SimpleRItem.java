package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class SimpleRItem implements RItem {
    private final @NotNull RRegistryRef<RItemType> typeRef;
    private final int amount;
    private final @NotNull RNbtCompound data;

    SimpleRItem(@NotNull RRegistryRef<RItemType> typeRef, int amount, @NotNull RNbtCompound data) {
        this.typeRef = Objects.requireNonNull(typeRef, "typeRef");
        this.amount = amount;
        this.data = Objects.requireNonNull(data, "data");
    }

    SimpleRItem(@NotNull RKey typeKey, int amount, @NotNull RNbtCompound data) {
        this(RItemType.ref(typeKey), amount, data);
    }

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

    @Override
    public @NotNull RItem withTypeKey(@NotNull RKey newTypeKey) {
        return new SimpleRItem(newTypeKey, amount, data);
    }

    @Override
    public @NotNull RItem withAmount(int newAmount) {
        return new SimpleRItem(typeRef, newAmount, data);
    }

    @Override
    public @NotNull RItem withData(@NotNull RNbtCompound data) {
        return new SimpleRItem(typeRef, amount, data);
    }

    @Override
    public void setTypeKey(@NotNull RKey newTypeKey) {
        throw new UnsupportedOperationException("SimpleRItem is immutable");
    }

    @Override
    public void setAmount(int newAmount) {
        throw new UnsupportedOperationException("SimpleRItem is immutable");
    }

    @Override
    public boolean isEmpty() {
        return amount <= 0;
    }

    @Override
    public int count() {
        return amount;
    }

    @Override
    public int maxStackSize() {
        return 64;
    }

    @Override
    public boolean isSimilar(@NotNull RItem other) {
        return typeKey().equals(other.typeKey()) && data.equals(other.data());
    }

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
