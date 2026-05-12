package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ParticleShape} that samples points along the edges of a polygon
 * defined by a list of vertices.
 */
final class PolygonParticleShape implements ParticleShape {
    private final List<RLocation> vertices;
    private final boolean closed;

    /**
     * Creates a polygon shape.
     *
     * @param vertices the polygon vertices
     * @param closed   whether to connect the last vertex back to the first
     */
    PolygonParticleShape(@NotNull List<RLocation> vertices, boolean closed) {
        this.vertices = List.copyOf(vertices);
        this.closed = closed;
    }

    @Override
    public @NotNull List<RLocation> sample(double density) {
        List<RLocation> points = new ArrayList<>();
        int segments = vertices.size() - (closed ? 0 : 1);
        for (int i = 0; i < segments; i++) {
            RLocation a = vertices.get(i);
            RLocation b = vertices.get((i + 1) % vertices.size());
            double dx = b.x() - a.x();
            double dy = b.y() - a.y();
            double dz = b.z() - a.z();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int count = Math.max(2, (int) (len * density));
            for (int j = 0; j < count; j++) {
                double t = (double) j / count;
                points.add(new RLocation(a.world(), a.x() + dx * t, a.y() + dy * t, a.z() + dz * t, 0f, 0f));
            }
        }
        return points;
    }
}
