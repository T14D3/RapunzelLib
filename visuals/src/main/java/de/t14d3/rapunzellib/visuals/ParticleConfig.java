package de.t14d3.rapunzellib.visuals;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for a particle visual.
 *
 * @param shape        the particle shape to render
 * @param color        the particle color
 * @param density      the particle density (particles per unit length/area)
 * @param viewDistance the maximum view distance in blocks
 */
public record ParticleConfig(
    @NotNull ParticleShape shape,
    @NotNull TextColor color,
    double density,
    double viewDistance
) implements VisualConfig {
    @Override
    public double viewDistance() {
        return viewDistance;
    }
}
