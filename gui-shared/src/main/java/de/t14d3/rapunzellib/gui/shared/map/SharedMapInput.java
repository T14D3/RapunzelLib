package de.t14d3.rapunzellib.gui.shared.map;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.AttackEntityPre;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
import de.t14d3.rapunzellib.events.interact.UseBlockPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.gui.map.GuiMapClick;

/**
 * Routes platform game events to open map sessions.
 * <p>
 * While a session is open, block interactions are cancelled and delivered to
 * the session as map clicks; opening the inventory closes the map; quitting
 * cleans the session up. This is purely the cross-loader event bus - no
 * platform API is involved.
 * </p>
 */
public final class SharedMapInput {

    private SharedMapInput() {
    }

    /**
     * Subscribes the map input handlers to the bus. Idempotent per bus
     * instance; call once at feature install.
     *
     * @param bus the game event bus
     */
    public static void wire(@org.jetbrains.annotations.NotNull GameEventBus bus) {
        bus.onPre(InteractBlockPre.class, SharedMapInput::onInteractBlock);
        bus.onPre(UseBlockPre.class, SharedMapInput::onUseBlock);
        bus.onPre(InteractEntityPre.class, SharedMapInput::onInteractEntity);
        bus.onPre(AttackEntityPre.class, SharedMapInput::onAttackEntity);
        bus.onPre(InventoryOpenPre.class, SharedMapInput::onInventoryOpen);
        bus.onPost(InventoryClosePost.class, SharedMapInput::onInventoryClose);
        bus.onPost(PlayerQuitPost.class, SharedMapInput::onPlayerQuit);
    }

    private static void onInteractBlock(InteractBlockPre event) {
        SharedMapSession session = SharedMapSessions.get(event.player().uuid());
        if (session == null) {
            return;
        }
        event.deny();
        if (event.hand() == InteractBlockPre.Hand.MAIN_HAND) {
            GuiMapClick.Action action = event.action() == InteractBlockPre.Action.LEFT_CLICK_BLOCK
                ? GuiMapClick.Action.LEFT
                : GuiMapClick.Action.RIGHT;
            session.activate(action);
        }
    }

    /**
     * A right-click on a block dispatches both {@link InteractBlockPre} and
     * {@link UseBlockPre}; the activation happens on the interact event only,
     * this just makes sure the underlying block use is cancelled too.
     */
    private static void onUseBlock(UseBlockPre event) {
        if (SharedMapSessions.get(event.player().uuid()) != null) {
            event.deny();
        }
    }

    /** Interacting with an entity (right-click) is a press on the map. */
    private static void onInteractEntity(InteractEntityPre event) {
        SharedMapSession session = SharedMapSessions.get(event.player().uuid());
        if (session == null) {
            return;
        }
        event.deny();
        session.activate(GuiMapClick.Action.RIGHT);
    }

    /** Attacking an entity (left-click) is a press on the map. */
    private static void onAttackEntity(AttackEntityPre event) {
        SharedMapSession session = SharedMapSessions.get(event.player().uuid());
        if (session == null) {
            return;
        }
        event.deny();
        session.activate(GuiMapClick.Action.LEFT);
    }

    private static void onInventoryOpen(InventoryOpenPre event) {
        SharedMapSessions.close(event.player().uuid());
    }

    private static void onInventoryClose(InventoryClosePost event) {
        SharedMapSessions.close(event.player().uuid());
    }

    private static void onPlayerQuit(PlayerQuitPost event) {
        SharedMapSessions.close(event.uuid());
    }
}
