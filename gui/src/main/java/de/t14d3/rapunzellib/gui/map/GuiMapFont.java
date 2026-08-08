package de.t14d3.rapunzellib.gui.map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A measurement and glyph source for text drawn on a map canvas.
 * <p>
 * Maps are 128x128 pixels, so fonts are expected to be tiny bitmap fonts.
 * The renderer provides a built-in 5x7 font; custom fonts can be supplied
 * per map for other scripts or styles.
 * </p>
 */
public interface GuiMapFont {

    /** The height in pixels of a line of text. */
    int lineHeight();

    /** The width in pixels of a single character. */
    int charWidth(char ch);

    /**
     * The width in pixels of the whole string, including the one-pixel
     * spacing the canvas applies between characters.
     *
     * @param text the text to measure
     * @return the width in pixels
     */
    default int widthOf(@NotNull String text) {
        if (text.isEmpty()) {
            return 0;
        }
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += charWidth(text.charAt(i)) + (i == text.length() - 1 ? 0 : 1);
        }
        return width;
    }

    /**
     * The pixel grid for a character, or {@code null} if the font does not
     * have it. The canvas substitutes the {@code '?'} glyph for unknown
     * characters, so a font must always provide one.
     *
     * @param ch the character
     * @return the glyph, or {@code null} if unavailable
     */
    @Nullable GuiMapGlyph glyph(char ch);
}
