package de.t14d3.rapunzellib.visuals.shared;

import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.visuals.ParticleConfig;
import de.t14d3.rapunzellib.visuals.ParticleShape;
import de.t14d3.rapunzellib.visuals.ParticleVisual;
import de.t14d3.rapunzellib.visuals.VisualAudience;
import de.t14d3.rapunzellib.visuals.VisualId;
import de.t14d3.rapunzellib.visuals.VisualManager;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * Shared NMS implementation of a particle visual.
 * <p>
 * Emits dust particles at sampled points of a configurable shape on each tick.
 * The particles are colored according to the config and respect view distance.
 */
public final class SharedNmsParticleVisual extends SharedNmsVisual<ParticleConfig> implements ParticleVisual {
    private ParticleShape currentShape;

    public SharedNmsParticleVisual(
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
    protected void spawnFor(@NotNull ServerPlayer player) {
    }

    @Override
    protected void destroyFor(@NotNull ServerPlayer player) {
    }

    /**
     * Emits particles for the current tick to all viewers.
     */
    public void emitTick() {
        List<RLocation> points = currentShape.sample(config.density());
        if (points.isEmpty()) return;

        DustParticleOptions particleOptions = createParticleOptions();
        for (UUID uuid : currentViewerIds()) {
            RPlayer player = RPlayer.get(uuid).orElse(null);
            if (player == null) continue;
            ServerPlayer serverPlayer = tryUnwrap(player);
            if (serverPlayer == null) continue;
            for (RLocation location : points) {
                if (!canSeeLocation(player, location, config.viewDistance())) continue;
                serverPlayer.connection.send(new ClientboundLevelParticlesPacket(
                    particleOptions,
                    false,
                    false,
                    location.x(), location.y(), location.z(),
                    0.0F, 0.0F, 0.0F,
                    0.0F,
                    1
                ));
            }
        }
    }

    private DustParticleOptions createParticleOptions() {
        TextColor color = config.color();
        int rgb = color != null ? color.value() : 0xFFFFFF;
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        return new DustParticleOptions(ARGB.colorFromFloat(1.0f, r, g, b), 1.0f);
    }
}
