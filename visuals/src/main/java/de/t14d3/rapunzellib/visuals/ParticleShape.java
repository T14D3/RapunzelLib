package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ParticleShape {
    @NotNull List<RLocation> sample(double density);

    static @NotNull ParticleShape point(@NotNull RLocation loc) {
        return density -> List.of(loc);
    }

    static @NotNull ParticleShape line(@NotNull RLocation from, @NotNull RLocation to) {
        return new LineParticleShape(from, to);
    }

    static @NotNull ParticleShape circle(@NotNull RLocation center, double radius, @NotNull Plane plane) {
        return new CircleParticleShape(center, radius, plane);
    }

    static @NotNull ParticleShape sphere(@NotNull RLocation center, double radius) {
        return new SphereParticleShape(center, radius);
    }

    static @NotNull ParticleShape box(@NotNull RLocation min, @NotNull RLocation max, double step) {
        return new BoxParticleShape(min, max, step);
    }

    static @NotNull ParticleShape polygon(@NotNull List<RLocation> vertices, boolean closed) {
        return new PolygonParticleShape(vertices, closed);
    }

    static @NotNull ParticleShape custom(@NotNull ParticleSampler sampler) {
        return sampler::sample;
    }
}
