package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class BoxStructureShape implements BlockStructureShape {
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final RWorldRef world;
    private final float thickness;

    BoxStructureShape(@NotNull RBlockPos corner1, @NotNull RBlockPos corner2, @NotNull RLocation worldLocation) {
        this.world = worldLocation.world();
        this.minX = Math.min(corner1.x(), corner2.x());
        this.minY = Math.min(corner1.y(), corner2.y());
        this.minZ = Math.min(corner1.z(), corner2.z());
        this.maxX = Math.max(corner1.x(), corner2.x());
        this.maxY = Math.max(corner1.y(), corner2.y());
        this.maxZ = Math.max(corner1.z(), corner2.z());
        this.thickness = 0.1f;
    }

    @Override
    public @NotNull List<Face> faces() {
        List<Face> result = new ArrayList<>(12);

        float width = maxX - minX;
        float height = maxY - minY;
        float depth = maxZ - minZ;

        if (width == 0 || height == 0 || depth == 0) {
            return result;
        }

        float t = thickness;

        // 4 edges along X axis
        result.add(new Face(new RLocation(world, minX, minY, minZ, 0f, 0f), new Vector3f(width, t, t)));
        result.add(new Face(new RLocation(world, minX, minY, maxZ - t, 0f, 0f), new Vector3f(width, t, t)));
        result.add(new Face(new RLocation(world, minX, maxY - t, minZ, 0f, 0f), new Vector3f(width, t, t)));
        result.add(new Face(new RLocation(world, minX, maxY - t, maxZ - t, 0f, 0f), new Vector3f(width, t, t)));

        // 4 edges along Y axis
        result.add(new Face(new RLocation(world, minX, minY, minZ, 0f, 0f), new Vector3f(t, height, t)));
        result.add(new Face(new RLocation(world, maxX - t, minY, minZ, 0f, 0f), new Vector3f(t, height, t)));
        result.add(new Face(new RLocation(world, minX, minY, maxZ - t, 0f, 0f), new Vector3f(t, height, t)));
        result.add(new Face(new RLocation(world, maxX - t, minY, maxZ - t, 0f, 0f), new Vector3f(t, height, t)));

        // 4 edges along Z axis
        result.add(new Face(new RLocation(world, minX, minY, minZ, 0f, 0f), new Vector3f(t, t, depth)));
        result.add(new Face(new RLocation(world, maxX - t, minY, minZ, 0f, 0f), new Vector3f(t, t, depth)));
        result.add(new Face(new RLocation(world, minX, maxY - t, minZ, 0f, 0f), new Vector3f(t, t, depth)));
        result.add(new Face(new RLocation(world, maxX - t, maxY - t, minZ, 0f, 0f), new Vector3f(t, t, depth)));

        return result;
    }
}
