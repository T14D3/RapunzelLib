package de.t14d3.rapunzellib.gui.map;

import org.jetbrains.annotations.NotNull;

/**
 * Immutable RGBA color used for map drawing.
 * <p>
 * Alpha is honored by the canvas: fully opaque colors are snapped to the map
 * palette directly, translucent colors are blended against whatever is already
 * on the canvas first. This is what makes selection tints over terrain work.
 * </p>
 */
public record GuiMapColor(int r, int g, int b, int a) {

    public static final GuiMapColor BLACK = of(0x000000);
    public static final GuiMapColor WHITE = of(0xFFFFFF);
    public static final GuiMapColor TRANSPARENT = new GuiMapColor(0, 0, 0, 0);

    public GuiMapColor {
        r = clamp(r);
        g = clamp(g);
        b = clamp(b);
        a = clamp(a);
    }

    /**
     * Creates an opaque color from an {@code 0xRRGGBB} value.
     *
     * @param rgb the packed RGB value
     * @return the color
     */
    public static @NotNull GuiMapColor of(int rgb) {
        return new GuiMapColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * Returns this color with the given alpha.
     *
     * @param alpha the new alpha, 0..255
     * @return the adjusted color
     */
    public @NotNull GuiMapColor withAlpha(int alpha) {
        return new GuiMapColor(r, g, b, alpha);
    }

    /**
     * Blends {@code weight} of the other color into this one.
     *
     * @param other  the color to blend in
     * @param weight how much of {@code other} to use, 0..1
     * @return the blended color
     */
    public @NotNull GuiMapColor mix(@NotNull GuiMapColor other, double weight) {
        double self = 1.0 - weight;
        return new GuiMapColor(
            (int) Math.round(r * self + other.r * weight),
            (int) Math.round(g * self + other.g * weight),
            (int) Math.round(b * self + other.b * weight),
            a
        );
    }

    /**
     * Scales the RGB channels by a brightness factor, keeping alpha.
     *
     * @param factor the brightness factor, 0..1
     * @return the darkened (or brightened) color
     */
    public @NotNull GuiMapColor scaled(double factor) {
        return new GuiMapColor((int) Math.round(r * factor), (int) Math.round(g * factor), (int) Math.round(b * factor), a);
    }

    @Override
    public String toString() {
        return String.format("#%02x%02x%02x%02x", r, g, b, a);
    }
}
