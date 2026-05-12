package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ParticleShape} that samples points on the edges of an axis-aligned box.
 * Only points on the boundary surface (within a small epsilon) are included.
 */
final class BoxParticleShape implements ParticleShape {
    private final RLocation min;
    private final RLocation max;
    private final double step;

    /**
     * Creates a box shape.
     *
     * @param min  the minimum corner
     * @param max  the maximum corner
     * @param step the step size between sample points
     */
    BoxParticleShape(@NotNull RLocation min, @NotNull RLocation max, double step) {
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public @NotNull List<RLocation> sample(double density) {
        double actualStep = step / Math.max(0.1, density);
        List<RLocation> points = new ArrayList<>();
        for (double x = min.x(); x <= max.x(); x += actualStep) {
            for (double y = min.y(); y <= max.y(); y += actualStep) {
                for (double z = min.z(); z <= max.z(); z += actualStep) {
                    boolean onEdge =
                        Math.abs(x - min.x()) < 0.001 || Math.abs(x - max.x()) < 0.001 ||
                        Math.abs(y - min.y()) < 0.001 || Math.abs(y - max.y()) < 0.001 ||
                        Math.abs(z - min.z()) < 0.001 || Math.abs(z - max.z()) < 0.001;
                    if (onEdge) {
                        points.add(new RLocation(min.world(), x, y, z, 0f, 0f));
                    }
                }
            }
        }
        return points;
    }
}
