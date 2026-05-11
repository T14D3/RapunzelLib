package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BlockStructureShape {

    @NotNull List<Face> faces();

    record Face(@NotNull RLocation center, @NotNull Vector3f scale) {
        public Face {
            if (scale.x() < 0 || scale.y() < 0 || scale.z() < 0) {
                throw new IllegalArgumentException("Scale components must be non-negative");
            }
        }
    }

    static @NotNull BlockStructureShape box(@NotNull RBlockPos corner1, @NotNull RBlockPos corner2, @NotNull RLocation worldRef) {
        return new BoxStructureShape(corner1, corner2, worldRef);
    }

    static @NotNull BlockStructureShape box(@NotNull RLocation corner1, @NotNull RLocation corner2) {
        return new BoxStructureShape(corner1.blockPos(), corner2.blockPos(), corner1);
    }

    static @NotNull BlockStructureShape wall(@NotNull RBlockPos corner1, @NotNull RBlockPos corner2, @NotNull RLocation worldRef) {
        return new WallStructureShape(corner1, corner2, worldRef);
    }

    static @NotNull BlockStructureShape wall(@NotNull RLocation corner1, @NotNull RLocation corner2) {
        return new WallStructureShape(corner1.blockPos(), corner2.blockPos(), corner1);
    }
}
