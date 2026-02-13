package de.t14d3.rapunzellib.gui.shared.dialog;

import de.t14d3.rapunzellib.gui.dialog.GuiDialogModel;
import org.jetbrains.annotations.NotNull;

public record SharedDialogRenderData(@NotNull GuiDialogModel model, @NotNull SharedDialogPayload payload) {
}
