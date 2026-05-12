package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ParticleShape} that samples points on the surface of a sphere
 * using a Fibonacci (golden-angle) distribution for near-uniform coverage.
 */
final class SphereParticleShape implements ParticleShape {
    private final RLocation center;
    private final double radius;

    /**
     * Creates a sphere shape.
     *
     * @param center the sphere center
     * @param radius the sphere radius
     */
    SphereParticleShape(@NotNull RLocation center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    @Override
    public @NotNull List<RLocation> sample(double density) {
        int count = Math.max(4, (int) (4 * Math.PI * radius * radius * density));
        List<RLocation> points = new ArrayList<>(count);
        double goldenAngle = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < count; i++) {
            double y = 1 - ((double) i / (count - 1)) * 2;
            double r = Math.sqrt(1 - y * y);
            double theta = goldenAngle * i;
            double x = Math.cos(theta) * r;
            double z = Math.sin(theta) * r;
            points.add(new RLocation(
                center.world(),
                center.x() + x * radius,
                center.y() + y * radius,
                center.z() + z * radius,
                0f, 0f
            ));
        }
        return points;
    }
}
