package de.t14d3.rapunzellib.visuals.sponge;

import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.visuals.ParticleConfig;
import de.t14d3.rapunzellib.visuals.ParticleShape;
import de.t14d3.rapunzellib.visuals.ParticleVisual;
import de.t14d3.rapunzellib.visuals.VisualAudience;
import de.t14d3.rapunzellib.visuals.VisualId;
import de.t14d3.rapunzellib.visuals.VisualManager;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.math.vector.Vector3d;

import java.util.List;
import java.util.UUID;

/**
 * Sponge API implementation of a particle visual.
 * <p>
 * Emits {@link ParticleTypes#DUST} particles at sampled points of a
 * configurable shape on each tick. Particles are sent per-viewer via
 * {@link ServerPlayer#spawnParticles(ParticleEffect, Vector3d, int)} and are
 * distance-filtered by the config's view distance.
 */
public final class SpongeParticleVisual extends SpongeVisual<ParticleConfig> implements ParticleVisual {

    private volatile ParticleShape currentShape;

    public SpongeParticleVisual(
        @NotNull VisualId id,
        @NotNull ParticleConfig config,
        @NotNull VisualAudience audience,
        @NotNull VisualManager manager
    ) {
        super(id, config, audience, manager);
        this.currentShape = config.shape();
    }

    @Override
    public void updateShape(@NotNull ParticleShape shape) {
        this.currentShape = shape;
    }

    @Override
    protected void spawnEntities() {
        // Particles are transient; emission happens on each manager tick.
    }

    @Override
    protected void despawnEntities() {
        // Nothing to clean up; particles vanish on their own.
    }

    /**
     * Emits particles for the current tick to all current viewers.
     * Called by the visual manager's repeating tick.
     */
    public void emitTick() {
        if (!shown) return;
        List<RLocation> points = currentShape.sample(config.density());
        if (points.isEmpty()) return;

        ParticleEffect effect = createParticleEffect();
        for (UUID uuid : currentViewerIds()) {
            RPlayer player = RPlayer.get(uuid).orElse(null);
            if (player == null) continue;
            ServerPlayer serverPlayer = tryUnwrap(player);
            if (serverPlayer == null) continue;
            for (RLocation location : points) {
                if (!canSeeLocation(player, location, config.viewDistance())) continue;
                serverPlayer.spawnParticles(effect, new Vector3d(location.x(), location.y(), location.z()), 1);
            }
        }
    }

    private ParticleEffect createParticleEffect() {
        TextColor color = config.color();
        int rgb = color != null ? color.value() : 0xFFFFFF;
        return ParticleEffect.builder()
            .type(ParticleTypes.DUST.get())
            .option(ParticleOptions.COLOR.get(), toSpongeColor(rgb))
            .scale(1.0)
            .build();
    }
}
