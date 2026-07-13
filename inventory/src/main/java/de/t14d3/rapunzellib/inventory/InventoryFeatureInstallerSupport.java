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

/**
 * Utility methods and supporting types for installing and wiring inventory features
 * across different Minecraft platforms.
 * <p>
 * Provides factory methods for creating {@link Inventories} instances and
 * {@link SlotInventoryAdapter slot-based inventory adapters}, as well as helpers
 * for slot validation and empty-item detection.
 */
public final class InventoryFeatureInstallerSupport {
    private InventoryFeatureInstallerSupport() {
    }

    /**
     * Registers a set of {@link InventoryWrapperFactory inventory wrapper factories}
     * against the given context and returns a ready-to-use {@link Inventories} instance.
     *
     * @param context          the Rapunzel context to register into
     * @param platformId       the platform identifier
     * @param wrapperFactories the list of wrapper factories for the platform
     * @return a new {@link Inventories} instance backed by the provided factories
     */
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

    /**
     * Creates an {@link InventoryWrapperFactory} that delegates slot-level access
     * to the provided {@link SlotInventoryAdapter}.
     *
     * @param platformId the platform identifier
     * @param adapter    the adapter that defines how the native handle is read/written
     * @param <H>        the native handle type
     * @param <N>        the native item stack type
     * @return a new wrapper factory for slot-based inventories
     */
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

    /**
     * Checks whether the given item represents an "empty" or "air" slot.
     *
     * @param item the item to test, may be null
     * @return true if the item is null, has zero or negative amount, or its material key is air-like
     */
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

    /**
     * Validates that a slot index is within the bounds of the given inventory size.
     *
     * @param slot the slot index to validate
     * @param size the inventory size
     * @return the validated slot index
     * @throws IndexOutOfBoundsException if the slot is out of range
     */
    public static int requireSlot(int slot, int size) {
        if (slot < 0 || slot >= size) {
            throw new IndexOutOfBoundsException("Slot " + slot + " out of bounds for inventory size " + size);
        }
        return slot;
    }

    /**
     * A functional interface for writing a native item stack to a native handle at a given slot.
     *
     * @param <H> the native handle type
     * @param <N> the native item stack type
     */
    @FunctionalInterface
    public interface SlotWriter<H, N> {
        
        void set(@NotNull H handle, int slot, @Nullable N item);
    }

    /**
     * Adapts a native inventory handle to slot-based read/write operations.
     * <p>
     * Bundles all platform-specific functions (size, get, set, clear, emptiness checks)
     * and the {@link ItemStackAdapter} needed to convert between native stacks and {@link RItem}.
     *
     * @param <H> the native handle type
     * @param <N> the native item stack type
     */
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

        /**
         * Creates a new {@link Builder} for configuring a {@link SlotInventoryAdapter}.
         *
         * @param handleType  the native handle class
         * @param itemAdapter the adapter for converting native item stacks to {@link RItem}
         * @param <H>         the native handle type
         * @param <N>         the native item stack type
         * @return a new builder instance
         */
        public static <H, N> @NotNull Builder<H, N> builder(
            @NotNull Class<H> handleType,
            @NotNull ItemStackAdapter<N> itemAdapter
        ) {
            return new Builder<>(handleType, itemAdapter);
        }

        public @NotNull Class<H> handleType() {
            return handleType;
        }

        /**
         * Returns the size of the inventory represented by the given handle.
         *
         * @param handle the native inventory handle
         * @return the number of slots
         */
        public int size(@NotNull H handle) {
            return sizeFunction.applyAsInt(handle);
        }

        /**
         * Retrieves the native item at the specified slot.
         *
         * @param handle the native inventory handle
         * @param slot   the slot index
         * @return the native item, or null if empty
         */
        public @Nullable N getItem(@NotNull H handle, int slot) {
            return itemGetter.apply(handle, slot);
        }

        /**
         * Places a native item at the specified slot.
         *
         * @param handle the native inventory handle
         * @param slot   the slot index
         * @param item   the native item to place, or null to clear
         */
        public void setItem(@NotNull H handle, int slot, @Nullable N item) {
            itemSetter.set(handle, slot, item);
        }

        public boolean hasClearAction() {
            return clearAction != null;
        }

        /**
         * Invokes the registered clear action, if any, to clear the entire inventory at once.
         *
         * @param handle the native inventory handle
         */
        public void clear(@NotNull H handle) {
            Consumer<? super H> action = clearAction;
            if (action != null) {
                action.accept(handle);
            }
        }

        /**
         * Tests whether the given native item should be treated as empty/air.
         *
         * @param item the native item to test, may be null
         * @return true if the item is null or matches the empty predicate
         */
        public boolean isEmptyItem(@Nullable N item) {
            return item == null || emptyItemPredicate.test(item);
        }

        /**
         * Returns a platform-sentinel native item representing an empty slot.
         *
         * @return the empty item sentinel, may be null
         */
        public @Nullable N emptyItem() {
            return emptyItemSupplier.get();
        }

        /**
         * Returns the item adapter used for converting between native stacks and {@link RItem}.
         *
         * @return the item adapter
         */
        public @NotNull ItemStackAdapter<N> itemAdapter() {
            return itemAdapter;
        }

        /**
         * Builder for constructing a {@link SlotInventoryAdapter} with a fluent API.
         *
         * @param <H> the native handle type
         * @param <N> the native item stack type
         */
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

            /**
             * Sets the function that returns the inventory size for a given handle.
             *
             * @param sizeFunction the size function
             * @return this builder
             */
            public @NotNull Builder<H, N> size(@NotNull ToIntFunction<? super H> sizeFunction) {
                this.sizeFunction = Objects.requireNonNull(sizeFunction, "sizeFunction");
                return this;
            }

            /**
             * Sets the function that retrieves a native item from a handle by slot.
             *
             * @param itemGetter the item getter function
             * @return this builder
             */
            public @NotNull Builder<H, N> getItem(@NotNull BiFunction<? super H, Integer, ? extends N> itemGetter) {
                this.itemGetter = Objects.requireNonNull(itemGetter, "itemGetter");
                return this;
            }

            /**
             * Sets the writer that places a native item into a handle at a given slot.
             *
             * @param itemSetter the item setter
             * @return this builder
             */
            public @NotNull Builder<H, N> setItem(@NotNull SlotWriter<? super H, N> itemSetter) {
                this.itemSetter = Objects.requireNonNull(itemSetter, "itemSetter");
                return this;
            }

            public @NotNull Builder<H, N> clear(@Nullable Consumer<? super H> clearAction) {
                this.clearAction = clearAction;
                return this;
            }

            /**
             * Sets the predicate that tests whether a native item represents an empty slot.
             *
             * @param emptyItemPredicate the emptiness predicate
             * @return this builder
             */
            public @NotNull Builder<H, N> isEmptyItem(@NotNull Predicate<? super N> emptyItemPredicate) {
                this.emptyItemPredicate = Objects.requireNonNull(emptyItemPredicate, "emptyItemPredicate");
                return this;
            }

            /**
             * Sets the supplier that provides the sentinel empty native item.
             *
             * @param emptyItemSupplier the empty item supplier
             * @return this builder
             */
            public @NotNull Builder<H, N> emptyItem(@NotNull Supplier<? extends N> emptyItemSupplier) {
                this.emptyItemSupplier = Objects.requireNonNull(emptyItemSupplier, "emptyItemSupplier");
                return this;
            }

            public @NotNull Builder<H, N> emptyItem(@Nullable N emptyItem) {
                this.emptyItemSupplier = () -> emptyItem;
                return this;
            }

            /**
             * Builds the {@link SlotInventoryAdapter} with the configured parameters.
             *
             * @return a new adapter instance
             */
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
