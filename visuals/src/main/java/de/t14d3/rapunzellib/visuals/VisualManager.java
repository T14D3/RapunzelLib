package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public interface VisualManager {
    void register(@NotNull Visual<?> visual);

    void unregister(@NotNull Visual<?> visual);

    @NotNull Collection<Visual<?>> all();

    @NotNull Optional<Visual<?>> find(@NotNull VisualId id);

    void removeAll();

    @NotNull ParticleVisual createParticle(@NotNull ParticleConfig config, @NotNull VisualAudience audience);

    @NotNull BlockDisplayVisual createBlockDisplay(
        @NotNull BlockDisplayConfig config,
        @NotNull RLocation location,
        @NotNull VisualAudience audience
    );

    @NotNull GlowOutlineVisual createGlowOutline(@NotNull GlowOutlineConfig config, @NotNull VisualAudience audience);

    @NotNull BeaconBeamVisual createBeaconBeam(@NotNull BeaconBeamConfig config, @NotNull VisualAudience audience);

    @NotNull BlockStructureVisual createBlockStructure(
        @NotNull BlockStructureConfig config,
        @NotNull VisualAudience audience
    );
}
