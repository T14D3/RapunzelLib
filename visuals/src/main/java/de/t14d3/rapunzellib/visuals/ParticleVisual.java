package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

/**
 * A visual that renders particles along a {@link ParticleShape}.
 */
public interface ParticleVisual extends Visual<ParticleConfig> {

    /**
     * Updates the shape of this particle visual in real time.
     *
     * @param shape the new particle shape
     */
    void updateShape(@NotNull ParticleShape shape);
}
