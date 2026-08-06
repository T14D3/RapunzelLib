package de.t14d3.rapunzellib.visuals.sponge;

import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.visuals.BlockDisplayConfig;
import de.t14d3.rapunzellib.visuals.BlockDisplayVisual;
import de.t14d3.rapunzellib.visuals.DisplayTransform;
import de.t14d3.rapunzellib.visuals.VisualAudience;
import de.t14d3.rapunzellib.visuals.VisualId;
import de.t14d3.rapunzellib.visuals.VisualManager;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.display.BlockDisplay;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * Sponge API implementation of a block display visual.
 * <p>
 * Spawns a real {@code minecraft:block_display} entity into the world at the
 * configured location. Show = spawn the entity, hide/remove = remove it.
 * Dynamic updates ({@link #updateTransform}, {@link #updateBlock},
 * {@link #updateColor}) are applied to the live entity through data keys.
 */
public final class SpongeBlockDisplayVisual extends SpongeVisual<BlockDisplayConfig> implements BlockDisplayVisual {

    private final RLocation location;

    private volatile BlockDisplay entity;
    private volatile RBlockType currentBlock;
    private volatile DisplayTransform currentTransform;

    public SpongeBlockDisplayVisual(
        @NotNull VisualId id,
        @NotNull BlockDisplayConfig config,
        @NotNull VisualAudience audience,
        @NotNull VisualManager manager,
        @NotNull RLocation location
    ) {
        super(id, config, audience, manager);
        this.location = location;
        this.currentBlock = config.block();
        this.currentTransform = config.transform();
    }

    @Override
    protected @NotNull RLocation visibilityCenter() {
        return location;
    }

    @Override
    protected void spawnEntities() {
        if (entity != null && !entity.isRemoved()) return;
        ServerWorld world = serverWorld(location.world());
        if (world == null) return;
        entity = spawnBlockDisplay(
            world,
            new org.spongepowered.math.vector.Vector3d(location.x(), location.y(), location.z()),
            currentBlock,
            currentTransform,
            config.glow()
        );
    }

    @Override
    protected void despawnEntities() {
        despawnTracked();
        entity = null;
    }

    @Override
    protected void ensureEntitiesSpawned() {
        // Re-spawn if the entity was removed (e.g. by a chunk unload).
        spawnEntities();
    }

    @Override
    public void updateTransform(@NotNull DisplayTransform transform) {
        this.currentTransform = transform;
        applyToEntity(() -> entity.offer(Keys.TRANSFORM, toSpongeTransform(transform)));
    }

    @Override
    public void updateBlock(@NotNull RBlockType block) {
        this.currentBlock = block;
        applyToEntity(() -> entity.offer(Keys.BLOCK_STATE, resolveBlockState(block)));
    }

    @Override
    public void updateColor(@NotNull TextColor color) {
        // Sponge exposes glow as a boolean (Keys.IS_GLOWING) but does not expose
        // the display entity's glow color override, so the color cannot be
        // applied to the entity itself. The update is accepted for API
        // compatibility; the stored config color is used by consumers.
        applyToEntity(() -> entity.offer(Keys.IS_GLOWING, config.glow()));
    }

    /**
     * Applies an update to the live entity if it is currently spawned.
     *
     * @param update the update action
     */
    private void applyToEntity(@NotNull Runnable update) {
        BlockDisplay display = entity;
        if (display != null && !display.isRemoved()) {
            update.run();
        }
    }

    /** Returns the live entity, or {@code null} if not shown. */
    public @Nullable BlockDisplay entity() {
        return entity;
    }
}
