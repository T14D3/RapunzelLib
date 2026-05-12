package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@link BlockStructureShape} that produces a single rectangular face
 * spanning between two corners. Handles degenerate axes by adjusting
 * the resulting face thickness accordingly.
 */
final class WallStructureShape implements BlockStructureShape {
    private final RBlockPos corner1, corner2;
    private final RWorldRef world;
    private final float thickness;

    /**
     * Creates a wall structure shape from two corner positions.
     *
     * @param corner1       the first corner
     * @param corner2       the second corner
     * @param worldLocation a reference location to derive the world
     */
    WallStructureShape(@NotNull RBlockPos corner1, @NotNull RBlockPos corner2, @NotNull RLocation worldLocation) {
        this.world = worldLocation.world();
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.thickness = 0.1f;
    }

    @Override
    public @NotNull List<Face> faces() {
        int x1 = Math.min(corner1.x(), corner2.x());
        int x2 = Math.max(corner1.x(), corner2.x());
        int y1 = Math.min(corner1.y(), corner2.y());
        int y2 = Math.max(corner1.y(), corner2.y());
        int z1 = Math.min(corner1.z(), corner2.z());
        int z2 = Math.max(corner1.z(), corner2.z());

        int dx = x2 - x1;
        int dy = y2 - y1;
        int dz = z2 - z1;

        if (dx == 0 && dz == 0) {
            return List.of(new Face(new RLocation(world, x1, y1, z1, 0f, 0f), new Vector3f(thickness, dy, thickness)));
        } else if (dx == 0 && dy == 0) {
            return List.of(new Face(new RLocation(world, x1, y1, z1, 0f, 0f), new Vector3f(thickness, thickness, dz)));
        } else if (dy == 0 && dz == 0) {
            return List.of(new Face(new RLocation(world, x1, y1, z1, 0f, 0f), new Vector3f(dx, thickness, thickness)));
        } else if (dx == 0) {
            return List.of(new Face(new RLocation(world, x1, y1, z1, 0f, 0f), new Vector3f(thickness, dy, dz)));
        } else if (dy == 0) {
            return List.of(new Face(new RLocation(world, x1, y1, z1, 0f, 0f), new Vector3f(dx, thickness, dz)));
        } else if (dz == 0) {
            return List.of(new Face(new RLocation(world, x1, y1, z1, 0f, 0f), new Vector3f(dx, dy, thickness)));
        }

        return List.of(new Face(new RLocation(world, x1, y1, z1, 0f, 0f), new Vector3f(dx, dy, dz)));
    }
}
