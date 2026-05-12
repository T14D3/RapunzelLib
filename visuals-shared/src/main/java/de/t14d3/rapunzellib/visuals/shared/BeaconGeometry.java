package de.t14d3.rapunzellib.visuals.shared;

import de.t14d3.rapunzellib.objects.RBlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for computing block positions in a beacon pyramid and glass column shape.
 * <p>
 * Used by {@link SharedNmsBeaconBeamVisual} to determine which blocks to
 * render as part of a beacon beam visual.
 */
public final class BeaconGeometry {
    private BeaconGeometry() {
    }

    /**
     * Computes the block positions for a beacon pyramid of the given levels.
     *
     * @param beacon the beacon block position
     * @param levels the number of pyramid levels (1-4)
     * @return the list of block positions in the pyramid
     */
    public static @NotNull List<RBlockPos> pyramid(@NotNull RBlockPos beacon, int levels) {
        List<RBlockPos> positions = new ArrayList<>();
        if (levels <= 0) return positions;
        for (int layer = 0; layer < levels; layer++) {
            int radius = levels - layer;
            int y = beacon.y() - 1 - layer;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    positions.add(new RBlockPos(beacon.x() + dx, y, beacon.z() + dz));
                }
            }
        }
        return positions;
    }

    /**
     * Computes the block positions for a glass column above a beacon.
     *
     * @param beacon the beacon block position
     * @param height the height of the column
     * @return the list of block positions in the column
     */
    public static @NotNull List<RBlockPos> glassColumn(@NotNull RBlockPos beacon, int height) {
        List<RBlockPos> positions = new ArrayList<>();
        for (int i = 1; i <= height; i++) {
            positions.add(new RBlockPos(beacon.x(), beacon.y() + i, beacon.z()));
        }
        return positions;
    }
}
