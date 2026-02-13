package de.t14d3.rapunzellib.gui.paper;

import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.GuiRendererSelectionSupport;
import de.t14d3.rapunzellib.gui.paper.dialog.DialogRenderer;
import de.t14d3.rapunzellib.gui.paper.inventory.InventoryRenderer;
import org.jetbrains.annotations.NotNull;

public final class PaperGuiRenderer {
    private static final GuiRenderer INVENTORY_RENDERER = InventoryRenderer.instance();
    private static final GuiRenderer DIALOG_RENDERER = DialogRenderer.instance();
    private static final GuiRenderer AUTO_RENDERER = GuiRendererSelectionSupport.autoRenderer(
        "paper-auto",
        INVENTORY_RENDERER,
        DIALOG_RENDERER
    );

    private PaperGuiRenderer() {
    }

    @NotNull
    public static GuiRenderer inventory() {
        return INVENTORY_RENDERER;
    }

    @NotNull
    public static GuiRenderer dialog() {
        return DIALOG_RENDERER;
    }

    @NotNull
    public static GuiRenderer auto() {
        return AUTO_RENDERER;
    }
}
