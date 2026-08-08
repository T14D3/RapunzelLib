package de.t14d3.rapunzellib.gui.map;

import org.jetbrains.annotations.NotNull;

/**
 * A layer of a map: a draw callback that paints part of the map.
 * <p>
 * Layers are drawn bottom-up in registration order, so a terrain layer first
 * and selection overlays on top of it. Layers receive the canvas and the
 * current viewport, and are repainted whenever the map needs a frame - on
 * terrain movement, cursor movement, or after a click. Because the canvas
 * only marks changed pixels, a layer that draws the same thing twice in a row
 * sends nothing.
 * </p>
 */
@FunctionalInterface
public interface GuiMapLayer {

    /**
     * Paints this layer.
     *
     * @param canvas   the canvas to draw onto
     * @param viewport the current viewport, for world-to-pixel transforms
     */
    void draw(@NotNull GuiMapCanvas canvas, @NotNull GuiMapViewport viewport);
}
