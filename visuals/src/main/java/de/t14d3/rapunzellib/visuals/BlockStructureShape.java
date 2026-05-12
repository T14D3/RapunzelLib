package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Defines a structural shape composed of block display faces.
 * <p>
 * Each face is a rectangular region defined by a center location and scale.
 * Static factory methods provide box and wall shapes.
 */
public interface BlockStructureShape {

    /**
     * Returns the list of faces composing this structure.
     *
     * @return the list of faces
     */
    @NotNull List<Face> faces();

    /**
     * A single rectangular face in a block structure.
     *
     * @param center the center location of the face
     * @param scale  the scale (width, height, depth) of the face, all components non-negative
     */
    record Face(@NotNull RLocation center, @NotNull Vector3f scale) {
        /**
         * Creates a face, validating that scale components are non-negative.
         *
         * @param center the center location
         * @param scale  the scale vector
         * @throws IllegalArgumentException if any scale component is negative
         */
        public Face {
            if (scale.x() < 0 || scale.y() < 0 || scale.z() < 0) {
                throw new IllegalArgumentException("Scale components must be non-negative");
            }
        }
    }

    /**
     * Creates a box structure between two block positions.
     *
     * @param corner1  the first corner
     * @param corner2  the second corner
     * @param worldRef a reference location used to derive the world
     * @return a box structure shape
     */
    static @NotNull BlockStructureShape box(@NotNull RBlockPos corner1, @NotNull RBlockPos corner2, @NotNull RLocation worldRef) {
        return new BoxStructureShape(corner1, corner2, worldRef);
    }

    /**
     * Creates a box structure between two world locations.
     *
     * @param corner1 the first corner
     * @param corner2 the second corner
     * @return a box structure shape
     */
    static @NotNull BlockStructureShape box(@NotNull RLocation corner1, @NotNull RLocation corner2) {
        return new BoxStructureShape(corner1.blockPos(), corner2.blockPos(), corner1);
    }

    /**
     * Creates a wall structure between two block positions.
     *
     * @param corner1  the first corner
     * @param corner2  the second corner
     * @param worldRef a reference location used to derive the world
     * @return a wall structure shape
     */
    static @NotNull BlockStructureShape wall(@NotNull RBlockPos corner1, @NotNull RBlockPos corner2, @NotNull RLocation worldRef) {
        return new WallStructureShape(corner1, corner2, worldRef);
    }

    /**
     * Creates a wall structure between two world locations.
     *
     * @param corner1 the first corner
     * @param corner2 the second corner
     * @return a wall structure shape
     */
    static @NotNull BlockStructureShape wall(@NotNull RLocation corner1, @NotNull RLocation corner2) {
        return new WallStructureShape(corner1.blockPos(), corner2.blockPos(), corner1);
    }
}
