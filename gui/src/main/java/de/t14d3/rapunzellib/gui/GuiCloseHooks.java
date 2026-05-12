package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.gui.context.CloseReason;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Utility for dispatching close events to GUI close handlers.
 * <p>
 * Checks whether a GUI implements {@link Handler} and delegates the close
 * event accordingly.
 * </p>
 */
public final class GuiCloseHooks {
    private GuiCloseHooks() {
    }

    /**
     * Fires the close event on the given GUI if it implements {@link Handler}.
     *
     * @param gui    the GUI to close
     * @param player the player whose GUI session is closing
     * @param reason the reason for the close
     */
    public static void close(@NotNull Gui gui, @NotNull RPlayer player, @NotNull CloseReason reason) {
        if (gui instanceof Handler handler) {
            handler.handleClose(player, reason);
        }
    }

    /**
     * Callback interface for GUIs that want to handle their own close events.
     */
    public interface Handler {
        /**
         * Called when the GUI is being closed.
         *
         * @param player the player whose GUI session is closing
         * @param reason the reason for the close
         */
        void handleClose(@NotNull RPlayer player, @NotNull CloseReason reason);
    }
}
