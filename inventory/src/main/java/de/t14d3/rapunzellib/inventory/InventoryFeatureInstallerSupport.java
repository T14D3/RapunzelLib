package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.InventoryInterop;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public final class InventoryFeatureInstallerSupport {
    private InventoryFeatureInstallerSupport() {
    }

    public static @NotNull Inventories registerInventories(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull List<? extends InventoryWrapperFactory<?>> wrapperFactories
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(wrapperFactories, "wrapperFactories");

        Inventories inventories = new DefaultInventories(platformId, wrapperFactories);
        context.registerLinked(Inventories.class, inventories, InventoryInterop.class);
        return inventories;
    }

    public static <H, N> @NotNull InventoryWrapperFactory<H> slotInventoryFactory(
        @NotNull PlatformId platformId,
        @NotNull SlotInventoryAdapter<H, N> adapter
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(adapter, "adapter");

        return new InventoryWrapperFactory<>() {
            @Override
            public @NotNull PlatformId platformId() {
                return platformId;
            }

            @Override
            public @NotNull Class<H> handleType() {
                return adapter.handleType();
            }

            @Override
            public @NotNull RInventory wrap(@NotNull H nativeInventory) {
                return new SlotAccessRInventory<>(platformId, nativeInventory, adapter);
            }
        };
    }


    public static boolean isEmptyItem(@Nullable RItem item) {
        if (item == null) {
            return true;
        }
        if (item.amount() <= 0) {
            return true;
        }
        String material = item.typeKey().asString().trim().toLowerCase(Locale.ROOT);
        return material.isEmpty() || material.equals("air") || material.endsWith(":air");
    }

    public static int requireSlot(int slot, int size) {
        if (slot < 0 || slot >= size) {
            throw new IndexOutOfBoundsException("Slot " + slot + " out of bounds for inventory size " + size);
        }
        return slot;
    }

    @FunctionalInterface
    public interface SlotWriter<H, N> {
        void set(@NotNull H handle, int slot, @Nullable N item);
    }

    public static final class SlotInventoryAdapter<H, N> {
        private final Class<H> handleType;
        private final ToIntFunction<? super H> sizeFunction;
        private final BiFunction<? super H, Integer, ? extends N> itemGetter;
        private final SlotWriter<? super H, N> itemSetter;
        private final @Nullable Consumer<? super H> clearAction;
        private final Predicate<? super N> emptyItemPredicate;
        private final Supplier<? extends N> emptyItemSupplier;
        private final ItemStackAdapter<N> itemAdapter;

        private SlotInventoryAdapter(
            @NotNull Class<H> handleType,
            @NotNull ToIntFunction<? super H> sizeFunction,
            @NotNull BiFunction<? super H, Integer, ? extends N> itemGetter,
            @NotNull SlotWriter<? super H, N> itemSetter,
            @Nullable Consumer<? super H> clearAction,
            @NotNull Predicate<? super N> emptyItemPredicate,
            @NotNull Supplier<? extends N> emptyItemSupplier,
            @NotNull ItemStackAdapter<N> itemAdapter
        ) {
            this.handleType = Objects.requireNonNull(handleType, "handleType");
            this.sizeFunction = Objects.requireNonNull(sizeFunction, "sizeFunction");
            this.itemGetter = Objects.requireNonNull(itemGetter, "itemGetter");
            this.itemSetter = Objects.requireNonNull(itemSetter, "itemSetter");
            this.clearAction = clearAction;
            this.emptyItemPredicate = Objects.requireNonNull(emptyItemPredicate, "emptyItemPredicate");
            this.emptyItemSupplier = Objects.requireNonNull(emptyItemSupplier, "emptyItemSupplier");
            this.itemAdapter = Objects.requireNonNull(itemAdapter, "itemAdapter");
        }

        public static <H, N> @NotNull Builder<H, N> builder(
            @NotNull Class<H> handleType,
            @NotNull ItemStackAdapter<N> itemAdapter
        ) {
            return new Builder<>(handleType, itemAdapter);
        }

        public @NotNull Class<H> handleType() {
            return handleType;
        }

        public int size(@NotNull H handle) {
            return sizeFunction.applyAsInt(handle);
        }

        public @Nullable N getItem(@NotNull H handle, int slot) {
            return itemGetter.apply(handle, slot);
        }

        public void setItem(@NotNull H handle, int slot, @Nullable N item) {
            itemSetter.set(handle, slot, item);
        }

        public boolean hasClearAction() {
            return clearAction != null;
        }

        public void clear(@NotNull H handle) {
            Consumer<? super H> action = clearAction;
            if (action != null) {
                action.accept(handle);
            }
        }

        public boolean isEmptyItem(@Nullable N item) {
            return item == null || emptyItemPredicate.test(item);
        }

        public @Nullable N emptyItem() {
            return emptyItemSupplier.get();
        }

        public @NotNull ItemStackAdapter<N> itemAdapter() {
            return itemAdapter;
        }

        public static final class Builder<H, N> {
            private final Class<H> handleType;
            private final ItemStackAdapter<N> itemAdapter;
            private ToIntFunction<? super H> sizeFunction;
            private BiFunction<? super H, Integer, ? extends N> itemGetter;
            private SlotWriter<? super H, N> itemSetter;
            private @Nullable Consumer<? super H> clearAction;
            private Predicate<? super N> emptyItemPredicate;
            private Supplier<? extends N> emptyItemSupplier;

            private Builder(@NotNull Class<H> handleType, @NotNull ItemStackAdapter<N> itemAdapter) {
                this.handleType = Objects.requireNonNull(handleType, "handleType");
                this.itemAdapter = Objects.requireNonNull(itemAdapter, "itemAdapter");
            }

            public @NotNull Builder<H, N> size(@NotNull ToIntFunction<? super H> sizeFunction) {
                this.sizeFunction = Objects.requireNonNull(sizeFunction, "sizeFunction");
                return this;
            }

            public @NotNull Builder<H, N> getItem(@NotNull BiFunction<? super H, Integer, ? extends N> itemGetter) {
                this.itemGetter = Objects.requireNonNull(itemGetter, "itemGetter");
                return this;
            }

            public @NotNull Builder<H, N> setItem(@NotNull SlotWriter<? super H, N> itemSetter) {
                this.itemSetter = Objects.requireNonNull(itemSetter, "itemSetter");
                return this;
            }

            public @NotNull Builder<H, N> clear(@Nullable Consumer<? super H> clearAction) {
                this.clearAction = clearAction;
                return this;
            }

            public @NotNull Builder<H, N> isEmptyItem(@NotNull Predicate<? super N> emptyItemPredicate) {
                this.emptyItemPredicate = Objects.requireNonNull(emptyItemPredicate, "emptyItemPredicate");
                return this;
            }

            public @NotNull Builder<H, N> emptyItem(@NotNull Supplier<? extends N> emptyItemSupplier) {
                this.emptyItemSupplier = Objects.requireNonNull(emptyItemSupplier, "emptyItemSupplier");
                return this;
            }

            public @NotNull Builder<H, N> emptyItem(@Nullable N emptyItem) {
                this.emptyItemSupplier = () -> emptyItem;
                return this;
            }

            public @NotNull SlotInventoryAdapter<H, N> build() {
                return new SlotInventoryAdapter<>(
                    handleType,
                    Objects.requireNonNull(sizeFunction, "sizeFunction"),
                    Objects.requireNonNull(itemGetter, "itemGetter"),
                    Objects.requireNonNull(itemSetter, "itemSetter"),
                    clearAction,
                    Objects.requireNonNull(emptyItemPredicate, "emptyItemPredicate"),
                    Objects.requireNonNull(emptyItemSupplier, "emptyItemSupplier"),
                    itemAdapter
                );
            }
        }
    }

    private static final class SlotAccessRInventory<H, N> extends RNativeHandle<H> implements RInventory {
        private final SlotInventoryAdapter<H, N> adapter;

        private SlotAccessRInventory(
            @NotNull PlatformId platformId,
            @NotNull H handle,
            @NotNull SlotInventoryAdapter<H, N> adapter
        ) {
            super(platformId, handle);
            this.adapter = Objects.requireNonNull(adapter, "adapter");
        }

        @Override
        public int size() {
            return adapter.size(handle());
        }

        @Override
        public @NotNull java.util.Optional<RItem> item(int slot) {
            InventoryFeatureInstallerSupport.requireSlot(slot, size());
            N nativeItem = adapter.getItem(handle(), slot);
            if (adapter.isEmptyItem(nativeItem)) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(adapter.itemAdapter().snapshot(nativeItem));
        }

        @Override
        public void setItem(int slot, @Nullable RItem item) {
            InventoryFeatureInstallerSupport.requireSlot(slot, size());
            N nativeItem = InventoryFeatureInstallerSupport.isEmptyItem(item)
                ? adapter.emptyItem()
                : adapter.itemAdapter().create(Objects.requireNonNull(item, "item"));
            adapter.setItem(handle(), slot, nativeItem);
        }

        @Override
        public void clear() {
            if (adapter.hasClearAction()) {
                adapter.clear(handle());
                return;
            }
            RInventory.super.clear();
        }
    }
}
