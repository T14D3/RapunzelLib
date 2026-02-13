package de.t14d3.rapunzellib.gui.neoforge;

import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.GuiRendererSelectionSupport;
import de.t14d3.rapunzellib.gui.neoforge.dialog.DialogRenderer;
import de.t14d3.rapunzellib.gui.neoforge.inventory.InventoryRenderer;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeGuiRenderer {
    private static final GuiRenderer INVENTORY_RENDERER = InventoryRenderer.instance();
    private static final DialogRenderer DIALOG_RENDERER = DialogRenderer.instance();
    private static final GuiRenderer AUTO_RENDERER = GuiRendererSelectionSupport.autoRenderer(
        "neoforge-auto",
        INVENTORY_RENDERER,
        DIALOG_RENDERER,
        GuiRendererSelectionSupport::prefersDialogRenderer,
        (gui, player) -> DIALOG_RENDERER.available(),
        () -> DIALOG_RENDERER.available()
            ? GuiRendererSelectionSupport.unionCapabilities(INVENTORY_RENDERER, DIALOG_RENDERER)
            : INVENTORY_RENDERER.capabilities()
    );

    private NeoForgeGuiRenderer() {
    }

    public static GuiRenderer inventory() {
        return INVENTORY_RENDERER;
    }

    public static GuiRenderer dialog() {
        return DIALOG_RENDERER;
    }

    public static GuiRenderer auto() {
        return AUTO_RENDERER;
    }
}
