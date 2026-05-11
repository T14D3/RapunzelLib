package de.t14d3.rapunzellib.visuals.paper;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.visuals.*;
import de.t14d3.rapunzellib.visuals.shared.SharedNmsBeaconBeamVisual;
import de.t14d3.rapunzellib.visuals.shared.SharedNmsBlockDisplayVisual;
import de.t14d3.rapunzellib.visuals.shared.SharedNmsBlockStructureVisual;
import de.t14d3.rapunzellib.visuals.shared.SharedNmsGlowOutlineVisual;
import de.t14d3.rapunzellib.visuals.shared.SharedNmsParticleVisual;
import de.t14d3.rapunzellib.visuals.shared.SharedNmsVisualManager;
import org.jetbrains.annotations.NotNull;

public final class PaperVisualManager extends SharedNmsVisualManager {
    public PaperVisualManager(@NotNull RapunzelContext context) {
        super(context);
    }

    @Override
    public @NotNull ParticleVisual createParticle(@NotNull ParticleConfig config, @NotNull VisualAudience audience) {
        SharedNmsParticleVisual v = new SharedNmsParticleVisual(new VisualId(), config, audience, this);
        register(v);
        return v;
    }

    @Override
    public @NotNull BlockDisplayVisual createBlockDisplay(
        @NotNull BlockDisplayConfig config,
        @NotNull RLocation location,
        @NotNull VisualAudience audience
    ) {
        SharedNmsBlockDisplayVisual v = new SharedNmsBlockDisplayVisual(new VisualId(), config, audience, this, location);
        register(v);
        return v;
    }

    @Override
    public @NotNull GlowOutlineVisual createGlowOutline(@NotNull GlowOutlineConfig config, @NotNull VisualAudience audience) {
        SharedNmsGlowOutlineVisual v = new SharedNmsGlowOutlineVisual(new VisualId(), config, audience, this);
        register(v);
        return v;
    }

    @Override
    public @NotNull BeaconBeamVisual createBeaconBeam(@NotNull BeaconBeamConfig config, @NotNull VisualAudience audience) {
        SharedNmsBeaconBeamVisual v = new SharedNmsBeaconBeamVisual(new VisualId(), config, audience, this);
        register(v);
        return v;
    }

    @Override
    public @NotNull BlockStructureVisual createBlockStructure(@NotNull BlockStructureConfig config, @NotNull VisualAudience audience) {
        SharedNmsBlockStructureVisual v = new SharedNmsBlockStructureVisual(new VisualId(), config, audience, this);
        register(v);
        return v;
    }
}
