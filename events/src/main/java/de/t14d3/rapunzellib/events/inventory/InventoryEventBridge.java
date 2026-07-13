package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.GameEvents;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Bridge for dispatching inventory-related events through the {@link GameEventBus}.
 *
 * <p>Provides static methods for dispatching click, open, and close inventory events,
 * handling the pre/post dispatch pattern with cancellation support.</p>
 */
public final class InventoryEventBridge {
    private InventoryEventBridge() {
    }

    /**
     * Dispatches an inventory click event, returning a {@link ClickDispatch} handle
     * for post-dispatch.
     *
     * @param player    the clicking player
     * @param inventory the inventory being clicked
     * @param slot      the slot index
     * @param clickType the type of click
     * @return a ClickDispatch for post-processing
     */
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

    /**
     * Dispatches an inventory close event.
     *
     * @param player    the player who closed the inventory
     * @param inventory the inventory that was closed
     */
    public static void dispatchClose(@NotNull RPlayer player, @NotNull RInventory inventory) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(inventory, "inventory");

        support().bus().dispatchPost(InventoryEventPayloads.closePost(player, inventory));
    }

    /**
     * Dispatches an inventory open pre-event.
     *
     * @param player    the player opening the inventory
     * @param inventory the inventory being opened
     * @return true if the open is allowed (not denied or cancelled)
     */
    public static boolean dispatchOpenPre(@NotNull RPlayer player, @NotNull RInventory inventory) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(inventory, "inventory");

        InventorySupport support = support();
        InventoryOpenPre pre = InventoryEventPayloads.openPre(player, inventory);
        support.bus().dispatchPre(pre);
        return !pre.isCancelled() && !pre.isDenied();
    }

    /**
     * Dispatches an inventory open post-event.
     *
     * @param player    the player who opened the inventory
     * @param inventory the inventory that was opened
     */
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

    /**
     * Handle for post-dispatch of an inventory click event.
     *
     * @param bus       the event bus
     * @param player    the clicking player
     * @param inventory the inventory that was clicked
     * @param slot      the slot index
     * @param clickType the type of click
     * @param cancelled whether the click was cancelled
     */
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

        /**
         * Dispatches the post-click event.
         */
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
