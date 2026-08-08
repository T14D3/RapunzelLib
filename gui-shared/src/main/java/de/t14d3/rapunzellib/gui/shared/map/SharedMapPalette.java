package de.t14d3.rapunzellib.gui.shared.map;

import de.t14d3.rapunzellib.gui.map.GuiMapColor;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quantizes {@link GuiMapColor} values to the 256 map palette indices the
 * client renders.
 * <p>
 * The table comes straight from the vanilla map color table via
 * {@link MapColor#getColorFromPackedId}, so whatever this class hands back is
 * byte-for-byte what the client displays for that index. Quantization is
 * nearest-neighbor over the usable entries, memoized per RGB so a solid
 * selection tint costs one search per color, not one per pixel.
 * </p>
 */
public final class SharedMapPalette {

    private static final int SIZE = 256;

    private final int[] colors = new int[SIZE];
    private final List<Integer> usable = new ArrayList<>();
    private final Map<Integer, Byte> cache = new HashMap<>();

    public SharedMapPalette() {
        for (int packedId = 0; packedId < SIZE; packedId++) {
            int color = MapColor.getColorFromPackedId(packedId);
            colors[packedId] = color;
            // Skip fully transparent and pure-black entries: they are unused
            // slots and would drag every dark color toward them.
            if (((color >>> 24) & 0xFF) != 0 && (color & 0xFFFFFF) != 0) {
                usable.add(packedId);
            }
        }
    }

    /**
     * The palette index for a color, snapped to the nearest entry.
     *
     * @param color the color to quantize
     * @return the packed palette index
     */
    public byte index(GuiMapColor color) {
        int rgb = (color.r() << 16) | (color.g() << 8) | color.b();
        Byte cached = cache.get(rgb);
        if (cached != null) {
            return cached;
        }
        byte best = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int packedId : usable) {
            int candidate = colors[packedId];
            int distance = distance(rgb, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = (byte) packedId;
            }
        }
        cache.put(rgb, best);
        return best;
    }

    /** The color the client renders for a palette index. */
    public GuiMapColor color(int packedId) {
        int argb = colors[packedId & 0xFF];
        return new GuiMapColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, 255);
    }

    private static int distance(int rgb1, int rgb2) {
        int dr = ((rgb1 >> 16) & 0xFF) - ((rgb2 >> 16) & 0xFF);
        int dg = ((rgb1 >> 8) & 0xFF) - ((rgb2 >> 8) & 0xFF);
        int db = (rgb1 & 0xFF) - (rgb2 & 0xFF);
        return dr * dr + dg * dg + db * db;
    }
}
