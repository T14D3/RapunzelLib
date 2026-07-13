package de.t14d3.rapunzellib.visuals.shared;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.visuals.AbstractVisualManager;
import de.t14d3.rapunzellib.visuals.Visual;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

/**
 * Abstract base for shared visual managers that tick visuals at 20 TPS.
 * <p>
 * Handles viewer refresh, particle emission ticks, and periodic beacon
 * beam refresh (every 600 ticks / 30 seconds).
 */
public abstract class SharedNmsVisualManager extends AbstractVisualManager {
    private final ScheduledTask tickTask;
    private int beaconTickCounter = 0;

    protected SharedNmsVisualManager(@NotNull RapunzelContext context) {
        this.tickTask = context.scheduler().runRepeating(
            Duration.ZERO,
            Duration.ofMillis(50),
            this::tick
        );
    }

    /**
     * Cleans up viewer state for a disconnected player across all visuals.
     *
     * @param uuid the player UUID
     */
    public final void cleanupForPlayer(@NotNull UUID uuid) {
        for (Visual<?> visual : all()) {
            if (visual instanceof SharedNmsVisual<?> nmsVisual) {
                nmsVisual.onViewerQuit(uuid);
            }
        }
    }

    private void tick() {
        beaconTickCounter++;
        boolean beaconRefresh = beaconTickCounter >= 600;
        if (beaconRefresh) {
            beaconTickCounter = 0;
        }

        for (Visual<?> visual : all()) {
            if (visual instanceof SharedNmsVisual<?> nmsVisual) {
                tickVisual(nmsVisual, beaconRefresh);
            }
        }
    }

    private void tickVisual(@NotNull SharedNmsVisual<?> visual, boolean beaconRefresh) {
        Collection<RPlayer> audience = visual.audience().resolve();

        if (visual.isShown()) {
            visual.refreshViewers(audience);
            if (!visual.hasCurrentViewers()) {
                visual.hide();
                return;
            }
            if (visual instanceof SharedNmsParticleVisual particleVisual) {
                particleVisual.emitTick();
            }
            if (beaconRefresh && visual instanceof SharedNmsBeaconBeamVisual beaconVisual) {
                beaconVisual.refresh();
            }
        } else if (visual.hasEligibleViewers(audience)) {
            visual.show();
        }
    }
}
