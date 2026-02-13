package de.t14d3.rapunzellib.gui.fabric;

import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.GuiRendererSelectionSupport;
import de.t14d3.rapunzellib.gui.fabric.dialog.DialogGuiRenderer;
import de.t14d3.rapunzellib.gui.fabric.inventory.InventoryGuiRenderer;
import org.jetbrains.annotations.NotNull;

public final class FabricGuiRenderer {
    private static final GuiRenderer INVENTORY_RENDERER = InventoryGuiRenderer.INSTANCE;
    private static final GuiRenderer DIALOG_RENDERER = DialogGuiRenderer.INSTANCE;
    private static final GuiRenderer AUTO_RENDERER = GuiRendererSelectionSupport.autoRenderer(
        "fabric-auto",
        INVENTORY_RENDERER,
        DIALOG_RENDERER,
        GuiRendererSelectionSupport::prefersDialogRenderer,
        (gui, player) -> DialogGuiRenderer.INSTANCE.available(player),
        () -> GuiRendererSelectionSupport.unionCapabilities(INVENTORY_RENDERER, DIALOG_RENDERER)
    );

    private FabricGuiRenderer() {
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
