package de.t14d3.rapunzellib.gui.shared.dialog;

import de.t14d3.rapunzellib.gui.dialog.GuiDialogModel;
import org.jetbrains.annotations.NotNull;

/**
 * Container for dialog render data, bundling the dialog model with its serialized payload.
 *
 * @param model   the dialog model
 * @param payload the shared dialog payload
 */
public record SharedDialogRenderData(@NotNull GuiDialogModel model, @NotNull SharedDialogPayload payload) {
}
