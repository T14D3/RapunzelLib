package de.t14d3.rapunzellib.gui.shared.map;

import de.t14d3.rapunzellib.gui.map.GuiMapRect;

/**
 * A byte-per-pixel canvas backing a map render.
 * <p>
 * Pixels are palette indices in the map color space. The surface tracks a
 * dirty rectangle: {@link #set} only expands it when a pixel actually
 * changes, so repainting a fully static frame dirties nothing and sends
 * nothing. This is what keeps full redraws cheap for the network - a
 * selection highlight moving around dirties only the highlight.
 * </p>
 */
public final class SharedMapSurface {

    private final int width;
    private final int height;
    private final byte[] pixels;

    private int dirtyMinX;
    private int dirtyMinY;
    private int dirtyMaxX;
    private int dirtyMaxY;

    public SharedMapSurface(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new byte[width * height];
        clearDirty();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public void set(int x, int y, byte color) {
        if (!inBounds(x, y)) {
            return;
        }
        int index = y * width + x;
        if (pixels[index] == color) {
            return;
        }
        pixels[index] = color;
        dirtyMinX = Math.min(dirtyMinX, x);
        dirtyMinY = Math.min(dirtyMinY, y);
        dirtyMaxX = Math.max(dirtyMaxX, x);
        dirtyMaxY = Math.max(dirtyMaxY, y);
    }

    public byte get(int x, int y) {
        return inBounds(x, y) ? pixels[y * width + x] : 0;
    }

    public void fill(byte color) {
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] != color) {
                pixels[i] = color;
            }
        }
        markAllDirty();
    }

    public void clear(byte color) {
        java.util.Arrays.fill(pixels, color);
        markAllDirty();
    }

    public byte[] pixels() {
        return pixels;
    }

    /** Copies a rectangle out row-major - the layout a map patch expects. */
    public byte[] region(GuiMapRect rect) {
        byte[] region = new byte[rect.width() * rect.height()];
        for (int row = 0; row < rect.height(); row++) {
            System.arraycopy(pixels, (rect.y() + row) * width + rect.x(), region, row * rect.width(), rect.width());
        }
        return region;
    }

    public boolean isDirty() {
        return dirtyMinX <= dirtyMaxX;
    }

    /** The dirty rectangle, or null if nothing changed. */
    public GuiMapRect dirtyRect() {
        return isDirty() ? new GuiMapRect(dirtyMinX, dirtyMinY, dirtyMaxX - dirtyMinX + 1, dirtyMaxY - dirtyMinY + 1) : null;
    }

    public void clearDirty() {
        dirtyMinX = width;
        dirtyMinY = height;
        dirtyMaxX = -1;
        dirtyMaxY = -1;
    }

    public void markAllDirty() {
        dirtyMinX = 0;
        dirtyMinY = 0;
        dirtyMaxX = width - 1;
        dirtyMaxY = height - 1;
    }
}
