package de.t14d3.rapunzellib.gui.shared.map;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.AttackEntityPre;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
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
        // Physical contact (pressure plates/tripwires) is not a deliberate map
        // click; let it through untouched.
        if (event.action() == InteractBlockPre.Action.STEP) {
            return;
        }
        // The merged interact event covers both the former InteractBlockPre and
        // UseBlockPre, so a single deny here cancels the block use as well.
        event.deny();
        GuiMapClick.Action action = event.action() == InteractBlockPre.Action.ATTACK
            ? GuiMapClick.Action.LEFT
            : GuiMapClick.Action.RIGHT;
        session.activate(action);
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
