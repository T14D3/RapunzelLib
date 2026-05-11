package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

public interface ParticleVisual extends Visual<ParticleConfig> {
    void updateShape(@NotNull ParticleShape shape);
}
