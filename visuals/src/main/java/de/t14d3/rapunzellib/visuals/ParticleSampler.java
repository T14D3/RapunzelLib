package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@FunctionalInterface
public interface ParticleSampler {
    @NotNull List<RLocation> sample(double density);
}
