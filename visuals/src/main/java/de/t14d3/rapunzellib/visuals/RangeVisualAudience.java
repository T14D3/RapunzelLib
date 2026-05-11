package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class RangeVisualAudience implements VisualAudience {
    private final RLocation center;
    private final double radius;
    private volatile Collection<RPlayer> cached = List.of();
    private volatile long lastResolve = 0L;
    private final Object lock = new Object();
    private static final long TTL_MS = 250L;

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

    public @NotNull RLocation center() {
        return center;
    }

    public double radius() {
        return radius;
    }
}
