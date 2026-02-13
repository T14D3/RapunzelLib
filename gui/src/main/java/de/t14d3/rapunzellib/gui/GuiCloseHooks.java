package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.gui.context.CloseReason;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

public final class GuiCloseHooks {
    private GuiCloseHooks() {
    }

    public static void close(@NotNull Gui gui, @NotNull RPlayer player, @NotNull CloseReason reason) {
        if (gui instanceof Handler handler) {
            handler.handleClose(player, reason);
        }
    }

    public interface Handler {
        void handleClose(@NotNull RPlayer player, @NotNull CloseReason reason);
    }
}
