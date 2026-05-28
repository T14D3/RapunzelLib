package de.t14d3.rapunzellib.objects.snapshot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class InventorySnapshot {
    private final @NotNull String inventoryType;
    private final int size;
    private final @NotNull List<SlotEntry> slots;

    public InventorySnapshot(@NotNull String inventoryType, int size, @NotNull List<SlotEntry> slots) {
        this.inventoryType = inventoryType;
        this.size = size;
        this.slots = List.copyOf(slots);
    }

    public @NotNull String inventoryType() { return inventoryType; }
    public int size() { return size; }
    public @NotNull List<SlotEntry> slots() { return slots; }

    public static @NotNull InventorySnapshot of(int size, @NotNull List<SlotEntry> slots, @NotNull String type) {
        return new InventorySnapshot(type, size, slots);
    }

    public static final class SlotEntry {
        private final int slot;
        private final @Nullable String itemNbt;

        public SlotEntry(int slot, @Nullable String itemNbt) {
            this.slot = slot;
            this.itemNbt = itemNbt;
        }

        public int slot() { return slot; }
        public @Nullable String itemNbt() { return itemNbt; }
    }
}
