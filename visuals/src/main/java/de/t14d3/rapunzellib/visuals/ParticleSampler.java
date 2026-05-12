package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Functional interface for custom particle sampling logic.
 * <p>
 * Implementations produce a list of locations based on a density parameter.
 * This can be used with {@link ParticleShape#custom(ParticleSampler)} to
 * create arbitrary particle shapes.
 */
@FunctionalInterface
public interface ParticleSampler {

    /**
     * Samples particle positions at the given density.
     *
     * @param density the density of points per unit length/area
     * @return the list of sampled locations
     */
    @NotNull List<RLocation> sample(double density);
}
