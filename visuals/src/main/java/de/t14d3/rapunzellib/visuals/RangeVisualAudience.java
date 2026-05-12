package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * A {@link VisualAudience} implementation that resolves to all online
 * players within a given radius of a center location.
 * <p>
 * Results are cached for a short TTL ({@value #TTL_MS} ms) to
 * avoid re-filtering on every call.
 */
public final class RangeVisualAudience implements VisualAudience {

    /** The center of the search area. */
    private final RLocation center;

    /** The search radius in blocks. */
    private final double radius;

    /** Cached resolved player collection. */
    private volatile Collection<RPlayer> cached = List.of();

    /** Timestamp of the last resolution. */
    private volatile long lastResolve = 0L;

    /** Lock for synchronized cache updates. */
    private final Object lock = new Object();

    /** Cache TTL in milliseconds. */
    private static final long TTL_MS = 250L;

    /**
     * Creates a range-based audience.
     *
     * @param center the center location
     * @param radius the search radius in blocks
     */
    public RangeVisualAudience(@NotNull RLocation center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    @Override
    public @NotNull Collection<RPlayer> resolve() {
        long now = System.currentTimeMillis();
        Collection<RPlayer> snapshot = cached;
        if (snapshot != null && now - lastResolve < TTL_MS) {
            return snapshot;
        }
        synchronized (lock) {
            if (cached != null && now - lastResolve < TTL_MS) {
                return cached;
            }
            cached = RPlayer.online().stream()
                .filter(p -> {
                    var loc = p.asEntity().flatMap(REntity::location).orElse(null);
                    if (loc == null) return false;
                    if (!loc.world().identifier().equals(center.world().identifier())) return false;
                    double dx = loc.x() - center.x();
                    double dy = loc.y() - center.y();
                    double dz = loc.z() - center.z();
                    return (dx * dx + dy * dy + dz * dz) <= radius * radius;
                })
                .toList();
            lastResolve = now;
            return cached;
        }
    }

    /**
     * Returns the center location of this range audience.
     *
     * @return the center location
     */
    public @NotNull RLocation center() {
        return center;
    }

    /**
     * Returns the radius of this range audience.
     *
     * @return the radius in blocks
     */
    public double radius() {
        return radius;
    }
}
