package de.t14d3.rapunzellib.visuals.shared;

import de.t14d3.rapunzellib.objects.RBlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class BeaconGeometry {
    private BeaconGeometry() {
    }

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

    public static @NotNull List<RBlockPos> glassColumn(@NotNull RBlockPos beacon, int height) {
        List<RBlockPos> positions = new ArrayList<>();
        for (int i = 1; i <= height; i++) {
            positions.add(new RBlockPos(beacon.x(), beacon.y() + i, beacon.z()));
        }
        return positions;
    }
}