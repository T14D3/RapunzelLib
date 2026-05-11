package de.t14d3.rapunzellib.visuals.sponge;

import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.visuals.*;
import org.jetbrains.annotations.NotNull;

public final class SpongeVisualManager extends AbstractVisualManager {
    public SpongeVisualManager() {
    }

    @Override
    public @NotNull ParticleVisual createParticle(@NotNull ParticleConfig config, @NotNull VisualAudience audience) {
        throw new UnsupportedOperationException("Sponge particle visuals not yet implemented");
    }

    @Override
    public @NotNull BlockDisplayVisual createBlockDisplay(
        @NotNull BlockDisplayConfig config,
        @NotNull RLocation location,
        @NotNull VisualAudience audience
    ) {
        throw new UnsupportedOperationException("Sponge block display visuals not yet implemented");
    }

    @Override
    public @NotNull GlowOutlineVisual createGlowOutline(@NotNull GlowOutlineConfig config, @NotNull VisualAudience audience) {
        throw new UnsupportedOperationException("Sponge glow outline visuals not yet implemented");
    }

    @Override
    public @NotNull BeaconBeamVisual createBeaconBeam(@NotNull BeaconBeamConfig config, @NotNull VisualAudience audience) {
        throw new UnsupportedOperationException("Sponge beacon beam visuals not yet implemented");
    }
}
