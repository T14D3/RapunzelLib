package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ParticleShape} that samples points along a line segment
 * between two locations in the same world.
 */
final class LineParticleShape implements ParticleShape {
    private final RLocation from;
    private final RLocation to;

    LineParticleShape(@NotNull RLocation from, @NotNull RLocation to) {
        if (!from.world().identifier().equals(to.world().identifier())) {
            throw new IllegalArgumentException("from and to must be in the same world");
        }
        this.from = from;
        this.to = to;
    }

    @Override
    public @NotNull List<RLocation> sample(double density) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double dz = to.z() - from.z();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int count = Math.max(2, (int) (length * density));
        List<RLocation> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double t = (double) i / (count - 1);
            points.add(new RLocation(
                from.world(),
                from.x() + dx * t,
                from.y() + dy * t,
                from.z() + dz * t,
                0f, 0f
            ));
        }
        return points;
    }
}
