package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

/**
 * An immutable block position in a world, represented by integer coordinates.
 *
 * @param x the x-coordinate
 * @param y the y-coordinate
 * @param z the z-coordinate
 */
public record RBlockPos(int x, int y, int z) {
}

