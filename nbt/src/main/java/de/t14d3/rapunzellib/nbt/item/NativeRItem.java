package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface NativeRItem<H> extends RItem, RNative {
    @Override
    @NotNull H handle();

    @FunctionalInterface
    interface NativeUpdater<H> {
        @NotNull H update(@NotNull H currentHandle, @NotNull RItem updatedItem);
    }

    static <H> @NotNull NativeRItem<H> of(
        @NotNull PlatformId platformId,
        @NotNull H handle,
        @NotNull RItem snapshot,
        @NotNull NativeUpdater<H> updater
    ) {
        return new DefaultNativeRItem<>(platformId, handle, snapshot, updater);
    }
}

final class DefaultNativeRItem<H> extends RNativeHandle<H> implements NativeRItem<H> {
    private final @NotNull RRegistryRef<RItemType> typeRef;
    private final int amount;
    private final @NotNull RNbtCompound data;
    private final @NotNull NativeUpdater<H> updater;

    DefaultNativeRItem(
        @NotNull PlatformId platformId,
        @NotNull H handle,
        @NotNull RItem snapshot,
        @NotNull NativeUpdater<H> updater
    ) {
        super(platformId, handle);
        this.typeRef = Objects.requireNonNull(snapshot.typeRef(), "typeRef");
        this.amount = snapshot.amount();
        this.data = Objects.requireNonNull(snapshot.data(), "data");
        this.updater = Objects.requireNonNull(updater, "updater");
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
        return copy(newTypeKey, amount, data);
    }

    @Override
    public @NotNull RItem withAmount(int newAmount) {
        return copy(typeRef.key(), newAmount, data);
    }

    @Override
    public @NotNull RItem withData(@NotNull RNbtCompound newData) {
        return copy(typeRef.key(), amount, newData);
    }

    private @NotNull NativeRItem<H> copy(@NotNull RKey newTypeKey, int newAmount, @NotNull RNbtCompound newData) {
        RItem updatedItem = new SimpleRItem(newTypeKey, newAmount, newData);
        return new DefaultNativeRItem<>(platformId(), updater.update(handle(), updatedItem), updatedItem, updater);
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
}
