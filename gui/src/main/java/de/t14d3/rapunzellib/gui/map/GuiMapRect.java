package de.t14d3.rapunzellib.gui.map;

/**
 * An axis-aligned rectangle on the map canvas, in surface pixels.
 * <p>
 * {@code width} and {@code height} are guaranteed non-negative; the right and
 * bottom edges are exclusive, matching the canvas pixel model.
 * </p>
 */
public record GuiMapRect(int x, int y, int width, int height) {

    public GuiMapRect {
        if (width < 0) {
            width = 0;
        }
        if (height < 0) {
            height = 0;
        }
    }

    /** The first x outside this rectangle. */
    public int right() {
        return x + width;
    }

    /** The first y outside this rectangle. */
    public int bottom() {
        return y + height;
    }

    public boolean contains(int px, int py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }

    /**
     * The intersection of this rectangle with another, possibly empty.
     *
     * @param other the other rectangle
     * @return the overlapping rectangle
     */
    public @org.jetbrains.annotations.NotNull GuiMapRect intersect(GuiMapRect other) {
        int minX = Math.max(x, other.x);
        int minY = Math.max(y, other.y);
        int maxX = Math.min(right(), other.right());
        int maxY = Math.min(bottom(), other.bottom());
        return new GuiMapRect(minX, minY, Math.max(0, maxX - minX), Math.max(0, maxY - minY));
    }
}
