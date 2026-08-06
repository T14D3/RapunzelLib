package de.t14d3.rapunzellib.visuals.sponge;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.visuals.DisplayTransform;
import de.t14d3.rapunzellib.visuals.GlowOutlineConfig;
import de.t14d3.rapunzellib.visuals.GlowOutlineVisual;
import de.t14d3.rapunzellib.visuals.Quaternionf;
import de.t14d3.rapunzellib.visuals.VisualAudience;
import de.t14d3.rapunzellib.visuals.VisualId;
import de.t14d3.rapunzellib.visuals.VisualManager;
import de.t14d3.rapunzellib.visuals.Vector3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.entity.display.BlockDisplay;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import java.util.HashMap;
import java.util.Map;

/**
 * Sponge API implementation of a glow outline visual.
 * <p>
 * Renders a glowing outline around a set of block positions using real
 * {@code minecraft:block_display} entities, one per block, with a slight
 * scale increase and negative translation to create the outline effect
 * (mirroring the shared NMS implementation).
 * <p>
 * {@link de.t14d3.rapunzellib.objects.RBlockPos} carries no world reference,
 * so the target world is derived from the first resolved audience member.
 */
public final class SpongeGlowOutlineVisual extends SpongeVisual<GlowOutlineConfig> implements GlowOutlineVisual {

    private final Map<RBlockPos, BlockDisplay> entities = new HashMap<>();

    public SpongeGlowOutlineVisual(
        @NotNull VisualId id,
        @NotNull GlowOutlineConfig config,
        @NotNull VisualAudience audience,
        @NotNull VisualManager manager
    ) {
        super(id, config, audience, manager);
    }

    @Override
    protected void spawnEntities() {
        ServerWorld world = audienceWorld();
        if (world == null) return;

        DisplayTransform outlineTransform = outlineTransform();
        for (RBlockPos pos : config.blocks()) {
            BlockDisplay existing = entities.get(pos);
            if (existing != null && !existing.isRemoved()) continue;
            Vector3d center = new Vector3d(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
            BlockDisplay display = spawnBlockDisplay(world, center, config.outlineBlock(), outlineTransform, true);
            entities.put(pos, display);
        }
    }

    @Override
    protected void despawnEntities() {
        despawnTracked();
        entities.clear();
    }

    @Override
    protected void ensureEntitiesSpawned() {
        // Re-spawn outline entities that were removed (e.g. by a chunk unload).
        spawnEntities();
    }

    /**
     * Derives the target world from the first resolved audience member.
     *
     * @return the target server world, or {@code null} if none can be resolved
     */
    private @Nullable ServerWorld audienceWorld() {
        for (RPlayer player : audience().resolve()) {
            RLocation location = player.asEntity().flatMap(e -> e.location()).orElse(null);
            if (location == null) continue;
            ServerWorld world = serverWorld(location.world());
            if (world != null) return world;
        }
        return null;
    }

    /**
     * Builds the outline transform: a slight scale increase with a negative
     * translation so the outline block wraps the target block.
     *
     * @return the outline display transform
     */
    private static DisplayTransform outlineTransform() {
        return new DisplayTransform(
            new Vector3f(-0.49F, -0.49F, -0.49F),
            new Vector3f(1.02F, 1.02F, 1.02F),
            new Quaternionf(),
            new Quaternionf()
        );
    }
}
