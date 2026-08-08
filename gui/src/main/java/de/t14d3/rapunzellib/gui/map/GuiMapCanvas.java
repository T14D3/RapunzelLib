package de.t14d3.rapunzellib.gui.map;

import org.jetbrains.annotations.NotNull;

/**
 * The drawing surface a {@link GuiMapLayer} paints onto.
 * <p>
 * All primitives clip to the canvas bounds. Translucent colors are blended
 * against the existing pixels (via {@link #colorAt}), which is what makes
 * overlay tints over terrain readable. The canvas is readable, so layers can
 * also blend over whatever a lower layer drew.
 * </p>
 */
public interface GuiMapCanvas {

    /** The canvas width in pixels. */
    int width();

    /** The canvas height in pixels. */
    int height();

    /** The font {@link #text(int, int, String, GuiMapColor)} draws with. */
    @NotNull GuiMapFont font();

    /**
     * Sets a single pixel, honoring alpha and clipping.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param color the color to draw
     */
    void pixel(int x, int y, @NotNull GuiMapColor color);

    /**
     * Reads back the currently drawn color at a pixel.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the color currently on the canvas
     */
    @NotNull GuiMapColor colorAt(int x, int y);

    /** Fills a rectangle with a flat color. */
    void fillRect(@NotNull GuiMapRect rect, @NotNull GuiMapColor color);

    /**
     * Draws the border of a rectangle.
     *
     * @param rect        the rectangle to outline
     * @param borderWidth the border thickness in pixels
     * @param color       the border color
     * @param radius      the corner radius in pixels, 0 for square corners
     */
    void outlineRect(@NotNull GuiMapRect rect, int borderWidth, @NotNull GuiMapColor color, int radius);

    /** Draws a line between two points using Bresenham's algorithm. */
    void line(int x1, int y1, int x2, int y2, @NotNull GuiMapColor color);

    /**
     * Draws a filled ellipse with an optional outline.
     *
     * @param centerX the center x
     * @param centerY the center y
     * @param radiusX the horizontal radius
     * @param radiusY the vertical radius
     * @param fill    the fill color, or {@code null} for none
     * @param outline the outline color, or {@code null} for none
     */
    void ellipse(int centerX, int centerY, int radiusX, int radiusY, GuiMapColor fill, GuiMapColor outline);

    /**
     * Draws a single line of text using the canvas's default font.
     *
     * @param x     the left edge
     * @param y     the top edge
     * @param text  the text to draw
     * @param color the text color
     */
    void text(int x, int y, @NotNull String text, @NotNull GuiMapColor color);

    /**
     * Draws a single line of text with an explicit font.
     *
     * @param x     the left edge
     * @param y     the top edge
     * @param text  the text to draw
     * @param color the text color
     * @param font  the font to use
     */
    void text(int x, int y, @NotNull String text, @NotNull GuiMapColor color, @NotNull GuiMapFont font);

    /** Clears the whole canvas to a color. */
    void clear(@NotNull GuiMapColor color);
}
