package de.t14d3.rapunzellib.gui.shared.map;

import de.t14d3.rapunzellib.gui.map.GuiMapCanvas;
import de.t14d3.rapunzellib.gui.map.GuiMapColor;
import de.t14d3.rapunzellib.gui.map.GuiMapFont;
import de.t14d3.rapunzellib.gui.map.GuiMapGlyph;
import de.t14d3.rapunzellib.gui.map.GuiMapRect;
import org.jetbrains.annotations.NotNull;

/**
 * Immediate-mode rasterizer over a {@link SharedMapSurface}.
 * <p>
 * All primitives are integer math: Bresenham lines, an exact inside-test
 * ellipse whose outline is derived from the fill mask, and boxes whose border
 * follows the corner shape by insetting the outer shape. Translucent colors
 * blend against the current pixel before quantization.
 * </p>
 */
public final class SharedMapPainter implements GuiMapCanvas {

    private final SharedMapSurface surface;
    private final SharedMapPalette palette;
    private final GuiMapFont defaultFont;

    public SharedMapPainter(SharedMapSurface surface, SharedMapPalette palette, GuiMapFont defaultFont) {
        this.surface = surface;
        this.palette = palette;
        this.defaultFont = defaultFont;
    }

    @Override
    public int width() {
        return surface.width();
    }

    @Override
    public int height() {
        return surface.height();
    }

    @Override
    public @NotNull GuiMapFont font() {
        return defaultFont;
    }

    @Override
    public void pixel(int x, int y, @NotNull GuiMapColor color) {
        if (!surface.inBounds(x, y) || color.a() == 0) {
            return;
        }
        if (color.a() == 255) {
            surface.set(x, y, palette.index(color));
        } else {
            GuiMapColor under = colorAt(x, y);
            GuiMapColor blended = under.mix(color, color.a() / 255.0);
            surface.set(x, y, palette.index(blended));
        }
    }

    @Override
    public @NotNull GuiMapColor colorAt(int x, int y) {
        return palette.color(surface.get(x, y));
    }

    @Override
    public void fillRect(@NotNull GuiMapRect rect, @NotNull GuiMapColor color) {
        for (int y = rect.y(); y < rect.bottom(); y++) {
            for (int x = rect.x(); x < rect.right(); x++) {
                pixel(x, y, color);
            }
        }
    }

    @Override
    public void outlineRect(@NotNull GuiMapRect rect, int borderWidth, @NotNull GuiMapColor color, int radius) {
        if (rect.width() <= 0 || rect.height() <= 0) {
            return;
        }
        int edge = Math.max(0, Math.min(borderWidth, Math.min(rect.width(), rect.height()) / 2));
        if (edge == 0) {
            return;
        }
        double outer = Math.min(radius, Math.min(rect.width(), rect.height()) / 2.0);
        double inner = Math.max(0, outer - edge);
        int innerWidth = rect.width() - 2 * edge;
        int innerHeight = rect.height() - 2 * edge;

        for (int j = 0; j < rect.height(); j++) {
            for (int i = 0; i < rect.width(); i++) {
                double px = i + 0.5;
                double py = j + 0.5;
                if (!insideCorner(px, py, rect.width(), rect.height(), outer)) {
                    continue;
                }
                // A pixel is on the border when it is inside the outer shape
                // but outside the same shape inset by the border width.
                if (!insideCorner(px - edge, py - edge, innerWidth, innerHeight, inner)) {
                    pixel(rect.x() + i, rect.y() + j, color);
                }
            }
        }
    }

    /** Whether a point is inside a box with rounded corners. Only the corner squares need deciding. */
    private static boolean insideCorner(double px, double py, double w, double h, double radius) {
        if (px < 0 || py < 0 || px > w || py > h) {
            return false;
        }
        if (radius <= 0) {
            return true;
        }
        double intoX = radius - Math.min(px, w - px);
        double intoY = radius - Math.min(py, h - py);
        if (intoX <= 0 || intoY <= 0) {
            return true;
        }
        return intoX * intoX + intoY * intoY <= radius * radius;
    }

    @Override
    public void line(int x1, int y1, int x2, int y2, @NotNull GuiMapColor color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int error = dx - dy;

        while (true) {
            pixel(x1, y1, color);
            if (x1 == x2 && y1 == y2) {
                break;
            }
            int doubled = error * 2;
            if (doubled > -dy) {
                error -= dy;
                x1 += sx;
            }
            if (doubled < dx) {
                error += dx;
                y1 += sy;
            }
        }
    }

    @Override
    public void ellipse(int centerX, int centerY, int radiusX, int radiusY, GuiMapColor fill, GuiMapColor outline) {
        if (radiusX < 0 || radiusY < 0) {
            return;
        }
        for (int j = -radiusY; j <= radiusY; j++) {
            for (int i = -radiusX; i <= radiusX; i++) {
                if (!insideEllipse(i, j, radiusX, radiusY)) {
                    continue;
                }
                boolean edge = outline != null && (
                    !insideEllipse(i - 1, j, radiusX, radiusY)
                        || !insideEllipse(i + 1, j, radiusX, radiusY)
                        || !insideEllipse(i, j - 1, radiusX, radiusY)
                        || !insideEllipse(i, j + 1, radiusX, radiusY)
                );
                if (edge) {
                    pixel(centerX + i, centerY + j, outline);
                } else if (fill != null) {
                    pixel(centerX + i, centerY + j, fill);
                }
            }
        }
    }

    private static boolean insideEllipse(int x, int y, int radiusX, int radiusY) {
        if (radiusX == 0) {
            return x == 0 && Math.abs(y) <= radiusY;
        }
        if (radiusY == 0) {
            return y == 0 && Math.abs(x) <= radiusX;
        }
        long rx = radiusX;
        long ry = radiusY;
        return (long) x * x * ry * ry + (long) y * y * rx * rx <= rx * rx * ry * ry;
    }

    @Override
    public void text(int x, int y, @NotNull String text, @NotNull GuiMapColor color) {
        text(x, y, text, color, defaultFont);
    }

    @Override
    public void text(int x, int y, @NotNull String text, @NotNull GuiMapColor color, @NotNull GuiMapFont font) {
        if (text.isEmpty()) {
            return;
        }
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            GuiMapGlyph glyph = font.glyph(ch);
            if (glyph == null) {
                glyph = font.glyph('?');
            }
            if (glyph != null) {
                for (int row = 0; row < glyph.height(); row++) {
                    for (int col = 0; col < glyph.width(); col++) {
                        if (glyph.pixelAt(col, row)) {
                            pixel(cursor + col, y + row, color);
                        }
                    }
                }
            }
            cursor += font.charWidth(ch) + 1;
        }
    }

    @Override
    public void clear(@NotNull GuiMapColor color) {
        byte index = palette.index(color);
        surface.clear(index);
    }
}
