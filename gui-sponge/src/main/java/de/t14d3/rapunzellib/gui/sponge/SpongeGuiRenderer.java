package de.t14d3.rapunzellib.gui.sponge;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.GuiCapability;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.GuiRendererSelectionSupport;
import de.t14d3.rapunzellib.gui.sponge.inventory.InventoryRenderer;
import de.t14d3.rapunzellib.gui.sponge.dialog.DialogRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class SpongeGuiRenderer {

    private static final InventoryRenderer INVENTORY_RENDERER = new InventoryRenderer();
    private static final DialogRenderer DIALOG_RENDERER = new DialogRenderer();
    private static final GuiRenderer AUTO_RENDERER = GuiRendererSelectionSupport.autoRenderer(
        "sponge-auto",
        INVENTORY_RENDERER,
        DIALOG_RENDERER,
        SpongeGuiRenderer::prefersDialogRenderer,
        (gui, player) -> DIALOG_RENDERER.isAvailable(),
        () -> DIALOG_RENDERER.isAvailable()
            ? GuiRendererSelectionSupport.unionCapabilities(INVENTORY_RENDERER, DIALOG_RENDERER)
            : INVENTORY_RENDERER.capabilities()
    );

    private SpongeGuiRenderer() {
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

    private static boolean prefersDialogRenderer(@NotNull Gui gui) {
        Set<GuiCapability> required = GuiRendererSelectionSupport.requiredCapabilities(gui);
        return required.stream().anyMatch(capability -> capability == GuiCapability.NATIVE_TEXT_INPUT
            || capability == GuiCapability.NATIVE_SLIDER
            || capability == GuiCapability.NATIVE_TOGGLE
            || capability == GuiCapability.NATIVE_DROPDOWN
            || capability == GuiCapability.MODAL
            || capability == GuiCapability.SCROLLABLE);
    }
}
