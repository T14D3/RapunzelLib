package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.GameEvents;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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
     * <p>The fine-grained {@link InventoryClickType} is mapped faithfully to
     * the {@link InventoryActionType} carried by the dispatched
     * {@link InventoryActionPre}/{@link InventoryActionPost} payloads.</p>
     *
     * @param player    the clicking player
     * @param inventory the inventory being clicked
     * @param slot      the raw slot index
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
        InventoryActionPre pre = InventoryEventPayloads.actionPre(
            player,
            inventory,
            slot,
            mapActionType(clickType)
        );
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

    private static @NotNull InventoryActionType mapActionType(@NotNull InventoryClickType clickType) {
        return switch (clickType) {
            case LEFT -> InventoryActionType.LEFT;
            case RIGHT -> InventoryActionType.RIGHT;
            case SHIFT_LEFT -> InventoryActionType.SHIFT_LEFT;
            case SHIFT_RIGHT -> InventoryActionType.SHIFT_RIGHT;
            case DOUBLE_CLICK -> InventoryActionType.DOUBLE_CLICK;
            case NUMBER_KEY_1 -> InventoryActionType.NUMBER_KEY;
            case DROP -> InventoryActionType.DROP;
            case CONTROL_DROP -> InventoryActionType.CONTROL_DROP;
            case MIDDLE -> InventoryActionType.MIDDLE;
            case SWAP_OFFHAND -> InventoryActionType.SWAP_OFFHAND;
            case UNKNOWN -> InventoryActionType.UNKNOWN;
        };
    }

    /**
     * Handle for post-dispatch of an inventory click event.
     *
     * @param bus       the event bus
     * @param player    the clicking player
     * @param inventory the inventory that was clicked
     * @param slot      the raw slot index
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
        }

        /**
         * Dispatches the post-click event.
         */
        public void post() {
            RItem current = slot >= 0 && slot < inventory.size() ? inventory.item(slot).orElse(null) : null;
            bus.dispatchPost(InventoryEventPayloads.actionPost(
                player,
                inventory,
                List.of(slot),
                mapActionType(clickType),
                null,
                current,
                cancelled
            ));
        }
    }

    private record InventorySupport(@NotNull GameEventBus bus) {
        private InventorySupport {
            Objects.requireNonNull(bus, "bus");
        }
    }
}
