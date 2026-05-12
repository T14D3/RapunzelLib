package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Defines a geometric shape that can be sampled to produce particle positions.
 * <p>
 * Implementations calculate a set of {@link RLocation} points along the shape's
 * surface or volume based on a density parameter. Static factory methods provide
 * common shapes such as points, lines, circles, spheres, boxes, and polygons.
 */
public interface ParticleShape {

    /**
     * Samples points along this shape at the given density.
     *
     * @param density the density of points per unit length/area
     * @return the list of sampled locations
     */
    @NotNull List<RLocation> sample(double density);

    /**
     * Creates a single-point shape at the given location.
     *
     * @param loc the point location
     * @return a point particle shape
     */
    static @NotNull ParticleShape point(@NotNull RLocation loc) {
        return density -> List.of(loc);
    }

    /**
     * Creates a line shape between two locations.
     *
     * @param from the start location
     * @param to   the end location
     * @return a line particle shape
     */
    static @NotNull ParticleShape line(@NotNull RLocation from, @NotNull RLocation to) {
        return new LineParticleShape(from, to);
    }

    /**
     * Creates a circular shape on the specified plane.
     *
     * @param center the center of the circle
     * @param radius the circle radius
     * @param plane  the plane the circle lies on
     * @return a circle particle shape
     */
    static @NotNull ParticleShape circle(@NotNull RLocation center, double radius, @NotNull Plane plane) {
        return new CircleParticleShape(center, radius, plane);
    }

    /**
     * Creates a spherical shape (fibonacci sphere distribution).
     *
     * @param center the sphere center
     * @param radius the sphere radius
     * @return a sphere particle shape
     */
    static @NotNull ParticleShape sphere(@NotNull RLocation center, double radius) {
        return new SphereParticleShape(center, radius);
    }

    /**
     * Creates a hollow box shape defined by two opposite corners.
     *
     * @param min  the minimum corner
     * @param max  the maximum corner
     * @param step the step size between sample points
     * @return a box particle shape
     */
    static @NotNull ParticleShape box(@NotNull RLocation min, @NotNull RLocation max, double step) {
        return new BoxParticleShape(min, max, step);
    }

    /**
     * Creates a polygon shape from a list of vertices.
     *
     * @param vertices the polygon vertices
     * @param closed   whether the polygon is closed (connects last vertex to first)
     * @return a polygon particle shape
     */
    static @NotNull ParticleShape polygon(@NotNull List<RLocation> vertices, boolean closed) {
        return new PolygonParticleShape(vertices, closed);
    }

    /**
     * Creates a custom shape backed by a {@link ParticleSampler}.
     *
     * @param sampler the sampler implementation
     * @return a custom particle shape
     */
    static @NotNull ParticleShape custom(@NotNull ParticleSampler sampler) {
        return sampler::sample;
    }
}
