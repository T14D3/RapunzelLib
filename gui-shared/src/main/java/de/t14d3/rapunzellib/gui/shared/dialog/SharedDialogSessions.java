package de.t14d3.rapunzellib.gui.shared.dialog;

import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.core.GuiSessionStore;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogFieldValues;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModel;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogStateSupport;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogSubmissionProcessor;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Manages pending dialog sessions keyed by player UUID.
 * <p>
 * Handles registration, clearing, and submission of dialog results.
 */
public final class SharedDialogSessions {
    private static final GuiSessionStore<PendingDialog> PENDING_DIALOGS = new GuiSessionStore<>();

    private SharedDialogSessions() {
    }

    /**
     * Registers a pending dialog for a player.
     *
     * @param playerId the player UUID
     * @param dialog   the pending dialog
     */
    public static void register(@NotNull UUID playerId, @NotNull PendingDialog dialog) {
        PENDING_DIALOGS.put(playerId, dialog);
    }

    /**
     * Clears the pending dialog for a player.
     *
     * @param playerId the player UUID
     */
    public static void clear(@NotNull UUID playerId) {
        PENDING_DIALOGS.remove(playerId);
    }

    /**
     * Submits dialog results for a player, processing the collected values
     * through the submission processor.
     *
     * @param playerId   the player UUID
     * @param submission the dialog submission
     */
    public static void submit(@NotNull UUID playerId, @NotNull SharedDialogSubmission submission) {
        PendingDialog dialog = PENDING_DIALOGS.remove(playerId);
        if (dialog == null) {
            return;
        }
        GuiDialogFieldValues values = GuiDialogStateSupport.collectSubmittedValues(dialog.model(), field -> submission.value(field.key()));
        GuiDialogSubmissionProcessor.submit(
            dialog.model(),
            values,
            dialog.player(),
            dialog.state()
        );
    }

    /**
     * Represents a dialog session awaiting submission.
     *
     * @param player the player
     * @param state  the GUI state
     * @param model  the dialog model
     */
    public record PendingDialog(@NotNull RPlayer player, @NotNull GuiState state, @NotNull GuiDialogModel model) {
    }
}
