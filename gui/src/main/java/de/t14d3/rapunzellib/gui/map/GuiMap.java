package de.t14d3.rapunzellib.gui.map;

import de.t14d3.rapunzellib.gui.Gui;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * A map-based GUI: live world terrain with draw-on-top layers and
 * coordinate-aware clicks.
 * <p>
 * This is the full capability surface of the map renderer, in addition to the
 * plain {@link Gui} rendering the renderer also supports. A map is built with
 * {@link #builder()} and opened with {@link #open}: a map item appears in the
 * player's hand showing the world around them, the registered layers draw
 * over it (selection boxes, region tints, buttons), and clicks arrive with
 * pixel and block coordinates for hit-testing in world space.
 * </p>
 */
public interface GuiMap extends Gui {

    /**
     * The layers to draw, bottom-up. The first layer is the background.
     *
     * @return the layers
     */
    @NotNull List<GuiMapLayer> layers();

    /**
     * The terrain zoom in blocks per pixel, or 0 to disable terrain.
     * <p>
     * Terrain is a live view centered on the player and refreshed as they
     * move; 1 shows one block per pixel. Overlays drawn through layers share
     * the same viewport as the terrain.
     * </p>
     *
     * @return the zoom, or 0 for no terrain
     */
    int terrainBlocksPerPixel();

    /**
     * The handler for map clicks, or {@code null} if the map is display-only.
     *
     * @return the click handler
     */
    @Nullable Consumer<GuiMapClick> onClick();

    /**
     * Creates a map builder.
     *
     * @return a new builder
     */
    static @NotNull GuiMapBuilder builder() {
        return new GuiMapBuilder();
    }
}
