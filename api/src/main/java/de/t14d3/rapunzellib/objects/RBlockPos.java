package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

/**
 * An immutable block position in a world, represented by integer coordinates.
 */
public record RBlockPos(int x, int y, int z) {
    /**
     * Creates a block position from integer coordinates.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param z the z-coordinate
     * @return the new block position
     */
    public static @NotNull RBlockPos of(int x, int y, int z) {
        return new RBlockPos(x, y, z);
    }
}

