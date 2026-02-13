package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.GameEvents;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class InventoryEventBridge {
    private InventoryEventBridge() {
    }

    public static @NotNull ClickDispatch dispatchClick(
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        int slot,
        @NotNull InventoryClickType clickType
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(clickType, "clickType");

        InventorySupport support = support();
        InventoryClickPre pre = InventoryEventPayloads.clickPre(player, inventory, slot, clickType);
        support.bus().dispatchPre(pre);
        return new ClickDispatch(support.bus(), player, inventory, slot, clickType, pre.isCancelled() || pre.isDenied());
    }

    public static void dispatchClose(@NotNull RPlayer player, @NotNull RInventory inventory) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(inventory, "inventory");

        support().bus().dispatchPost(InventoryEventPayloads.closePost(player, inventory));
    }

    public static boolean dispatchOpenPre(@NotNull RPlayer player, @NotNull RInventory inventory) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(inventory, "inventory");

        InventorySupport support = support();
        InventoryOpenPre pre = InventoryEventPayloads.openPre(player, inventory);
        support.bus().dispatchPre(pre);
        return !pre.isCancelled() && !pre.isDenied();
    }

    public static void dispatchOpen(@NotNull RPlayer player, @NotNull RInventory inventory) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(inventory, "inventory");

        support().bus().dispatchPost(InventoryEventPayloads.openPost(player, inventory));
    }

    private static @NotNull InventorySupport support() {
        GameEventBus bus = Rapunzel.context().services().find(GameEventBus.class)
            .orElseGet(GameEvents::install);
        return new InventorySupport(bus);
    }

    public record ClickDispatch(
        @NotNull GameEventBus bus,
        @NotNull RPlayer player,
        @NotNull RInventory inventory,
        int slot,
        @NotNull InventoryClickType clickType,
        boolean cancelled
    ) {
        public ClickDispatch {
            Objects.requireNonNull(bus, "bus");
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(inventory, "inventory");
            Objects.requireNonNull(clickType, "clickType");
            if (slot < 0 || slot >= inventory.size()) {
                throw new IndexOutOfBoundsException("Slot " + slot + " out of bounds for inventory size " + inventory.size());
            }
        }

        public void post() {
            bus.dispatchPost(InventoryEventPayloads.clickPost(player, inventory, slot, clickType, cancelled));
        }
    }

    private record InventorySupport(@NotNull GameEventBus bus) {
        private InventorySupport {
            Objects.requireNonNull(bus, "bus");
        }
    }
}
