package de.t14d3.rapunzellib.visuals.sponge;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.visuals.AbstractVisualManager;
import de.t14d3.rapunzellib.visuals.BeaconBeamConfig;
import de.t14d3.rapunzellib.visuals.BeaconBeamVisual;
import de.t14d3.rapunzellib.visuals.BlockDisplayConfig;
import de.t14d3.rapunzellib.visuals.BlockDisplayVisual;
import de.t14d3.rapunzellib.visuals.GlowOutlineConfig;
import de.t14d3.rapunzellib.visuals.GlowOutlineVisual;
import de.t14d3.rapunzellib.visuals.ParticleConfig;
import de.t14d3.rapunzellib.visuals.ParticleVisual;
import de.t14d3.rapunzellib.visuals.Visual;
import de.t14d3.rapunzellib.visuals.VisualAudience;
import de.t14d3.rapunzellib.visuals.VisualId;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * Sponge implementation of the {@link de.t14d3.rapunzellib.visuals.VisualManager}.
 * <p>
 * Creates Sponge API based visual implementations and ticks them at 50 ms
 * intervals on the server's primary thread: particle visuals emit per-tick,
 * entity-based visuals are kept alive only while eligible viewers exist.
 */
public final class SpongeVisualManager extends AbstractVisualManager {

    private final RapunzelContext context;
    private final ScheduledTask tickTask;

    public SpongeVisualManager(@NotNull RapunzelContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.tickTask = context.scheduler().runRepeating(
            Duration.ZERO,
            Duration.ofMillis(50),
            this::tick
        );
    }

    @Override
    public @NotNull ParticleVisual createParticle(@NotNull ParticleConfig config, @NotNull VisualAudience audience) {
        SpongeParticleVisual visual = new SpongeParticleVisual(new VisualId(), config, audience, this);
        register(visual);
        return visual;
    }

    @Override
    public @NotNull BlockDisplayVisual createBlockDisplay(
        @NotNull BlockDisplayConfig config,
        @NotNull RLocation location,
        @NotNull VisualAudience audience
    ) {
        SpongeBlockDisplayVisual visual = new SpongeBlockDisplayVisual(new VisualId(), config, audience, this, location);
        register(visual);
        return visual;
    }

    @Override
    public @NotNull GlowOutlineVisual createGlowOutline(@NotNull GlowOutlineConfig config, @NotNull VisualAudience audience) {
        SpongeGlowOutlineVisual visual = new SpongeGlowOutlineVisual(new VisualId(), config, audience, this);
        register(visual);
        return visual;
    }

    @Override
    public @NotNull BeaconBeamVisual createBeaconBeam(@NotNull BeaconBeamConfig config, @NotNull VisualAudience audience) {
        SpongeBeaconBeamVisual visual = new SpongeBeaconBeamVisual(new VisualId(), config, audience, this);
        register(visual);
        return visual;
    }

    @Override
    public void cleanupForPlayer(@NotNull UUID uuid) {
        for (Visual<?> visual : all()) {
            if (visual instanceof SpongeVisual<?> spongeVisual) {
                spongeVisual.onViewerQuit(uuid);
            }
        }
    }

    /**
     * Repeating tick: refreshes viewers, auto-hides visuals without viewers,
     * and drives per-tick particle emission.
     */
    private void tick() {
        for (Visual<?> visual : all()) {
            if (visual instanceof SpongeVisual<?> spongeVisual) {
                tickVisual(spongeVisual);
            }
        }
    }

    private void tickVisual(@NotNull SpongeVisual<?> visual) {
        Collection<RPlayer> audience = visual.audience().resolve();

        if (visual.isShown()) {
            visual.refreshViewers(audience);
            if (!visual.hasCurrentViewers()) {
                visual.hide();
                return;
            }
            visual.ensureEntitiesSpawned();
            if (visual instanceof SpongeParticleVisual particleVisual) {
                particleVisual.emitTick();
            }
        } else if (visual.hasEligibleViewers(audience)) {
            visual.show();
        }
    }
}
