package de.t14d3.rapunzellib.gui.map;

/**
 * A single glyph of a map font: a small boolean pixel grid, row-major.
 * <p>
 * {@code pixels[row * width + column]} is {@code true} where the glyph is
 * drawn. Fonts are responsible for rendering their own glyph bitmaps; the
 * built-in map font ships with the renderer and custom fonts can be supplied
 * per map.
 * </p>
 */
public record GuiMapGlyph(int width, int height, boolean[] pixels) {

    public GuiMapGlyph {
        if (width < 0 || height < 0 || pixels.length != width * height) {
            throw new IllegalArgumentException("glyph pixels must be width*height");
        }
    }

    public boolean pixelAt(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height && pixels[y * width + x];
    }
}
