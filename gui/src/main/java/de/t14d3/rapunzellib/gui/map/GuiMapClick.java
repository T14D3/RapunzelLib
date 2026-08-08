package de.t14d3.rapunzellib.gui.map;

import de.t14d3.rapunzellib.objects.RBlockPos;
import org.jetbrains.annotations.NotNull;

/**
 * A click on a map, delivered to the map's click handler.
 * <p>
 * Clicks arrive with both the pixel on the canvas and the block column the
 * pixel covers, so consumers can work in world coordinates directly - which
 * is what a region selection tool wants. The {@code y} of the block position
 * is always zero: a map is a two-dimensional view of a block column.
 * </p>
 */
public record GuiMapClick(@NotNull GuiMapPoint pixel, @NotNull RBlockPos block, @NotNull Action action) {

    /** The mouse button that produced the click. */
    public enum Action {
        LEFT,
        RIGHT
    }
}
