package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtField;
import de.t14d3.rapunzellib.nbt.RNbtPath;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public interface NativeRItem<H> extends RItem, RNative {
    @Override
    @NotNull H handle();

    @FunctionalInterface
    interface NativeUpdater<H> {
        void apply(@NotNull H handle, @NotNull RItem mutation);
    }

    @FunctionalInterface
    interface SnapshotReader<H> {
        @NotNull RItem read(@NotNull H handle);
    }

    static <H> @NotNull NativeRItem<H> of(
        @NotNull PlatformId platformId,
        @NotNull H handle,
        @NotNull SnapshotReader<H> snapshotReader,
        @NotNull NativeUpdater<H> updater
    ) {
        return new LegacyNativeRItem<>(platformId, handle, snapshotReader, updater);
    }

    static <H> @NotNull NativeRItem<H> of(
        @NotNull PlatformId platformId,
        @NotNull H handle,
        @NotNull NativeRItemAccessor<H> accessor
    ) {
        return new DefaultNativeRItem<>(platformId, handle, accessor);
    }
}

final class DefaultNativeRItem<H> extends RNativeHandle<H> implements NativeRItem<H> {
    private final @NotNull NativeRItemAccessor<H> accessor;

    DefaultNativeRItem(
        @NotNull PlatformId platformId,
        @NotNull H handle,
        @NotNull NativeRItemAccessor<H> accessor
    ) {
        super(platformId, handle);
        this.accessor = Objects.requireNonNull(accessor, "accessor");
    }

    @Override
    public @NotNull RRegistryRef<RItemType> typeRef() {
        return accessor.typeRef(handle());
    }

    @Override
    public int amount() {
        return accessor.amount(handle());
    }

    @Override
    public @NotNull RNbtCompound data() {
        return accessor.data(handle());
    }

    @Override
    public @NotNull RItem withTypeKey(@NotNull RKey newTypeKey) {
        RRegistryRef<RItemType> newTypeRef = RItemType.ref(newTypeKey);
        RNbtCompound currentData = accessor.data(handle());
        int currentAmount = accessor.amount(handle());
        H newHandle = accessor.createHandle(newTypeRef, currentAmount);
        accessor.setData(newHandle, currentData);
        updateNativeHandle(newHandle);
        return this;
    }

    @Override
    public @NotNull RItem withAmount(int newAmount) {
        accessor.setAmount(handle(), newAmount);
        return this;
    }

    @Override
    public @NotNull RItem withData(@NotNull RNbtCompound newData) {
        accessor.setData(handle(), newData);
        return this;
    }

    @Override
    public void setTypeKey(@NotNull RKey newTypeKey) {
        withTypeKey(newTypeKey);
    }

    @Override
    public void setAmount(int newAmount) {
        withAmount(newAmount);
    }

    @Override
    public void setName(@Nullable Component name) {
        accessor.setName(handle(), name);
    }

    @Override
    public void setLore(@NotNull List<Component> lore) {
        accessor.setLore(handle(), lore);
    }

    @Override
    public @NotNull RItem withName(@Nullable Component name) {
        accessor.setName(handle(), name);
        return this;
    }

    @Override
    public @NotNull RItem withLore(@NotNull List<Component> lore) {
        accessor.setLore(handle(), lore);
        return this;
    }

    @Override
    public @NotNull RItem withDurability(int durability) {
        accessor.setDamage(handle(), durability);
        return this;
    }

    @Override
    public @NotNull RItem withUnbreakable(boolean unbreakable) {
        accessor.setUnbreakable(handle(), unbreakable);
        return this;
    }

    @Override
    public @NotNull RItem withCustomModelData(int modelData) {
        accessor.setCustomModelData(handle(), Integer.valueOf(modelData));
        return this;
    }

    @Override
    public @NotNull RItem withCustomModelData(@Nullable Integer modelData) {
        accessor.setCustomModelData(handle(), modelData);
        return this;
    }

    @Override
    public @NotNull RItem withRepairCost(int repairCost) {
        accessor.setRepairCost(handle(), Integer.valueOf(repairCost));
        return this;
    }

    @Override
    public @NotNull RItem withRepairCost(@Nullable Integer repairCost) {
        accessor.setRepairCost(handle(), repairCost);
        return this;
    }

    @Override
    public @NotNull RItem withEnchantmentGlintOverride(@Nullable Boolean enchantmentGlintOverride) {
        accessor.setEnchantmentGlintOverride(handle(), enchantmentGlintOverride);
        return this;
    }

    @Override
    public <T> @NotNull RItem with(@NotNull RNbtField<T> field, @NotNull T value) {
        accessor.setData(handle(), field.write(accessor.data(handle()), value));
        return this;
    }

    @Override
    public <T> @NotNull RItem with(@NotNull RNbtPath<T> path, @NotNull T value) {
        accessor.setData(handle(), path.write(accessor.data(handle()), value));
        return this;
    }

    @Override
    public @NotNull RItem without(@NotNull RNbtField<?> field) {
        accessor.setData(handle(), field.remove(accessor.data(handle())));
        return this;
    }

    @Override
    public @NotNull RItem without(@NotNull RNbtPath<?> path) {
        accessor.setData(handle(), path.remove(accessor.data(handle())));
        return this;
    }

    @Override
    public boolean isEmpty() {
        return accessor.isEmpty(handle());
    }

    @Override
    public int count() {
        return accessor.count(handle());
    }

    @Override
    public int maxStackSize() {
        return accessor.maxStackSize(handle());
    }

    @Override
    public boolean isSimilar(@NotNull RItem other) {
        return accessor.isSimilar(handle(), other);
    }

    @Override
    public @NotNull RItem withCount(int count) {
        accessor.setAmount(handle(), count);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RItem that)) {
            return false;
        }
        return amount() == that.amount() && typeKey().equals(that.typeKey()) && data().equals(that.data());
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeKey(), amount(), data());
    }
}

final class LegacyNativeRItem<H> extends RNativeHandle<H> implements NativeRItem<H> {
    private final @NotNull SnapshotReader<H> snapshotReader;
    private final @NotNull NativeUpdater<H> updater;

    LegacyNativeRItem(
        @NotNull PlatformId platformId,
        @NotNull H handle,
        @NotNull SnapshotReader<H> snapshotReader,
        @NotNull NativeUpdater<H> updater
    ) {
        super(platformId, handle);
        this.snapshotReader = Objects.requireNonNull(snapshotReader, "snapshotReader");
        this.updater = Objects.requireNonNull(updater, "updater");
    }

    @Override
    public @NotNull RRegistryRef<RItemType> typeRef() {
        return snapshotReader.read(handle()).typeRef();
    }

    @Override
    public int amount() {
        return snapshotReader.read(handle()).amount();
    }

    @Override
    public @NotNull RNbtCompound data() {
        return snapshotReader.read(handle()).data();
    }

    @Override
    public @NotNull RItem withTypeKey(@NotNull RKey newTypeKey) {
        RItem mutation = new SimpleRItem(newTypeKey, amount(), data());
        updater.apply(handle(), mutation);
        return this;
    }

    @Override
    public @NotNull RItem withAmount(int newAmount) {
        RItem mutation = new SimpleRItem(typeKey(), newAmount, data());
        updater.apply(handle(), mutation);
        return this;
    }

    @Override
    public @NotNull RItem withData(@NotNull RNbtCompound newData) {
        RItem mutation = new SimpleRItem(typeKey(), amount(), newData);
        updater.apply(handle(), mutation);
        return this;
    }

    @Override
    public void setTypeKey(@NotNull RKey newTypeKey) {
        withTypeKey(newTypeKey);
    }

    @Override
    public void setAmount(int newAmount) {
        withAmount(newAmount);
    }

    @Override
    public void setName(@Nullable Component name) {
        RItem mutation = withName(name);
        updater.apply(handle(), mutation);
    }

    @Override
    public void setLore(@NotNull List<Component> lore) {
        RItem mutation = withLore(lore);
        updater.apply(handle(), mutation);
    }

    @Override
    public <T> @NotNull RItem with(@NotNull RNbtField<T> field, @NotNull T value) {
        RNbtCompound newData = field.write(data(), value);
        RItem mutation = new SimpleRItem(typeKey(), amount(), newData);
        updater.apply(handle(), mutation);
        return this;
    }

    @Override
    public <T> @NotNull RItem with(@NotNull RNbtPath<T> path, @NotNull T value) {
        RNbtCompound newData = path.write(data(), value);
        RItem mutation = new SimpleRItem(typeKey(), amount(), newData);
        updater.apply(handle(), mutation);
        return this;
    }

    @Override
    public @NotNull RItem without(@NotNull RNbtField<?> field) {
        RNbtCompound newData = field.remove(data());
        RItem mutation = new SimpleRItem(typeKey(), amount(), newData);
        updater.apply(handle(), mutation);
        return this;
    }

    @Override
    public @NotNull RItem without(@NotNull RNbtPath<?> path) {
        RNbtCompound newData = path.remove(data());
        RItem mutation = new SimpleRItem(typeKey(), amount(), newData);
        updater.apply(handle(), mutation);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return snapshotReader.read(handle()).isEmpty();
    }

    @Override
    public int count() {
        return snapshotReader.read(handle()).count();
    }

    @Override
    public int maxStackSize() {
        return snapshotReader.read(handle()).maxStackSize();
    }

    @Override
    public boolean isSimilar(@NotNull RItem other) {
        return snapshotReader.read(handle()).isSimilar(other);
    }

    @Override
    public @NotNull RItem withCount(int count) {
        RItem mutation = new SimpleRItem(typeKey(), count, data());
        updater.apply(handle(), mutation);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RItem that)) {
            return false;
        }
        return amount() == that.amount() && typeKey().equals(that.typeKey()) && data().equals(that.data());
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeKey(), amount(), data());
    }
}
