package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class CircleParticleShape implements ParticleShape {
    private final RLocation center;
    private final double radius;
    private final Plane plane;

    CircleParticleShape(@NotNull RLocation center, double radius, @NotNull Plane plane) {
        this.center = center;
        this.radius = radius;
        this.plane = plane;
    }

    @Override
    public @NotNull List<RLocation> sample(double density) {
        double circumference = 2 * Math.PI * radius;
        int count = Math.max(3, (int) (circumference * density));
        List<RLocation> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double dx = 0, dy = 0, dz = 0;
            switch (plane) {
                case XY -> {
                    dx = Math.cos(angle) * radius;
                    dy = Math.sin(angle) * radius;
                }
                case XZ -> {
                    dx = Math.cos(angle) * radius;
                    dz = Math.sin(angle) * radius;
                }
                case YZ -> {
                    dy = Math.cos(angle) * radius;
                    dz = Math.sin(angle) * radius;
                }
            }
            points.add(new RLocation(center.world(), center.x() + dx, center.y() + dy, center.z() + dz, 0f, 0f));
        }
        return points;
    }
}
