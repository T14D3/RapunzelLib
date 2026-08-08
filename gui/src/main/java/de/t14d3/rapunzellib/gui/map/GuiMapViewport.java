package de.t14d3.rapunzellib.gui.map;

import de.t14d3.rapunzellib.objects.RBlockPos;
import org.jetbrains.annotations.NotNull;

/**
 * Maps between surface pixels and world blocks for a map view.
 * <p>
 * A viewport is centered on a block column and shows {@code blocksPerPixel}
 * blocks per pixel, so zoom is software-side: 1 shows one block per pixel,
 * 2 shows a 2x2 block column per pixel, and so on. Terrain and overlays share
 * the same viewport, which is why a selection rectangle drawn in world
 * coordinates lines up with the ground underneath.
 * </p>
 */
public record GuiMapViewport(int centerBlockX, int centerBlockZ, int blocksPerPixel, int width, int height) {

    public GuiMapViewport {
        blocksPerPixel = Math.max(1, blocksPerPixel);
    }

    /**
     * Creates a viewport for a canvas of the given size.
     *
     * @param centerBlockX   the block x the canvas is centered on
     * @param centerBlockZ   the block z the canvas is centered on
     * @param blocksPerPixel the zoom, in blocks per pixel
     * @param width          the canvas width in pixels
     * @param height         the canvas height in pixels
     * @return the viewport
     */
    public static @NotNull GuiMapViewport of(int centerBlockX, int centerBlockZ, int blocksPerPixel, int width, int height) {
        return new GuiMapViewport(centerBlockX, centerBlockZ, blocksPerPixel, width, height);
    }

    /**
     * Returns a viewport with the same zoom and size, centered elsewhere.
     *
     * @param blockX the new center block x
     * @param blockZ the new center block z
     * @return the moved viewport
     */
    public @NotNull GuiMapViewport withCenter(int blockX, int blockZ) {
        return new GuiMapViewport(blockX, blockZ, blocksPerPixel, width, height);
    }

    /**
     * Converts a block coordinate to the pixel of the cell covering it.
     * <p>
     * Blocks on the same pixel cell (e.g. all four blocks of a 2x2 column)
     * map to the same pixel, so this is a many-to-one projection.
     * </p>
     *
     * @param blockX the block x
     * @param blockZ the block z
     * @return the covering pixel
     */
    public @NotNull GuiMapPoint worldToPixel(int blockX, int blockZ) {
        return new GuiMapPoint(
            width / 2 + Math.floorDiv(blockX - centerBlockX, blocksPerPixel),
            height / 2 + Math.floorDiv(blockZ - centerBlockZ, blocksPerPixel)
        );
    }

    /**
     * Converts a pixel to the block column at its top-left corner.
     *
     * @param pixel the pixel
     * @return the block column the pixel covers
     */
    public @NotNull RBlockPos pixelToWorld(@NotNull GuiMapPoint pixel) {
        return new RBlockPos(
            centerBlockX + (pixel.x() - width / 2) * blocksPerPixel,
            0,
            centerBlockZ + (pixel.y() - height / 2) * blocksPerPixel
        );
    }
}
