package de.t14d3.rapunzellib.gui.shared.dialog;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModel;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModelBuilder;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public final class SharedDialogRenderSupport {
    private SharedDialogRenderSupport() {
    }

    public static @NotNull SharedDialogRenderData prepare(
        @NotNull Gui gui,
        @NotNull RenderContext context,
        @NotNull Component defaultTitle
    ) {
        GuiDialogModel model = GuiDialogModelBuilder.build(gui, defaultTitle);
        return new SharedDialogRenderData(model, SharedDialogPayloads.create(model, context));
    }
}
