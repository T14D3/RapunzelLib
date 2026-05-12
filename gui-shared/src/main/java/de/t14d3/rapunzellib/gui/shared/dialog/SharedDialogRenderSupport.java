package de.t14d3.rapunzellib.gui.shared.dialog;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModel;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModelBuilder;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Support utility for preparing dialog render data from a GUI and render context.
 */
public final class SharedDialogRenderSupport {
    private SharedDialogRenderSupport() {
    }

    /**
     * Prepares a dialog's render data by building the model and serializing the payload.
     *
     * @param gui           the GUI
     * @param context       the render context
     * @param defaultTitle  the default title if the GUI has none
     * @return the render data
     */
    public static @NotNull SharedDialogRenderData prepare(
        @NotNull Gui gui,
        @NotNull RenderContext context,
        @NotNull Component defaultTitle
    ) {
        GuiDialogModel model = GuiDialogModelBuilder.build(gui, defaultTitle);
        return new SharedDialogRenderData(model, SharedDialogPayloads.create(model, context));
    }
}
