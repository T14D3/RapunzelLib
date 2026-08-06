package de.t14d3.rapunzellib.visuals.sponge;

import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.visuals.DisplayTransform;
import de.t14d3.rapunzellib.visuals.Quaternionf;
import de.t14d3.rapunzellib.visuals.Visual;
import de.t14d3.rapunzellib.visuals.VisualAudience;
import de.t14d3.rapunzellib.visuals.VisualConfig;
import de.t14d3.rapunzellib.visuals.VisualId;
import de.t14d3.rapunzellib.visuals.VisualManager;
import de.t14d3.rapunzellib.visuals.Vector3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.display.BlockDisplay;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base for all Sponge API based visual implementations.
 * <p>
 * Unlike the shared NMS visuals (which send per-viewer packets), Sponge visuals
 * render through real world entities (block displays) and per-viewer particle
 * calls. This class manages the common visual lifecycle (show/hide/remove),
 * the current viewer set used for distance-based visibility, and the native
 * handle conversions required to talk to the Sponge API.
 *
 * <p>Note: this class deliberately implements {@link Visual} directly instead of
 * extending {@link de.t14d3.rapunzellib.visuals.AbstractVisual} because
 * {@code AbstractVisual.remove()} only flips the internal visibility flag and
 * never calls {@link #hide()}; entity-based Sponge visuals must despawn their
 * world entities on {@code remove()}, which requires overriding that method.</p>
 *
 * @param <C> the visual configuration type
 */
public abstract class SpongeVisual<C extends VisualConfig> implements Visual<C> {

    /** The unique visual identifier. */
    protected final VisualId id;

    /** The visual configuration. */
    protected final C config;

    /** The audience this visual targets. */
    protected final VisualAudience audience;

    /** Whether the visual is currently shown. */
    protected volatile boolean shown = false;

    /** The set of player UUIDs currently viewing this visual. */
    protected final Set<UUID> currentViewers = ConcurrentHashMap.newKeySet();

    /** The display entities currently owned by this visual. */
    private final Set<org.spongepowered.api.entity.Entity> trackedEntities = ConcurrentHashMap.newKeySet();

    private final VisualManager manager;

    protected SpongeVisual(
        @NotNull VisualId id,
        @NotNull C config,
        @NotNull VisualAudience audience,
        @NotNull VisualManager manager
    ) {
        this.id = id;
        this.config = config;
        this.audience = audience;
        this.manager = manager;
    }

    @Override
    public final @NotNull VisualId id() {
        return id;
    }

    @Override
    public final @NotNull C config() {
        return config;
    }

    @Override
    public final @NotNull VisualAudience audience() {
        return audience;
    }

    @Override
    public final boolean isShown() {
        return shown;
    }

    @Override
    public final void show() {
        if (shown) return;
        shown = true;
        try {
            spawnEntities();
        } catch (RuntimeException ex) {
            // A failed spawn (e.g. the target world is unloaded) must not leave
            // the visual in a permanently "shown" state.
            shown = false;
            currentViewers.clear();
            throw ex;
        }
        refreshViewers(audience.resolve());
    }

    @Override
    public final void hide() {
        if (!shown) return;
        shown = false;
        currentViewers.clear();
        despawnEntities();
    }

    @Override
    public final void remove() {
        hide();
        manager.unregister(this);
    }

    /**
     * Called when a viewer disconnects, to clean up tracking state.
     *
     * @param uuid the viewer UUID
     */
    public final void onViewerQuit(@NotNull UUID uuid) {
        currentViewers.remove(uuid);
    }

    /**
     * Checks whether there are any current viewers.
     *
     * @return {@code true} if there are viewers
     */
    public final boolean hasCurrentViewers() {
        return !currentViewers.isEmpty();
    }

    /**
     * Checks whether any of the resolved audience members are eligible viewers.
     *
     * @param resolvedAudience the resolved audience
     * @return {@code true} if at least one eligible viewer exists
     */
    public final boolean hasEligibleViewers(@NotNull Collection<RPlayer> resolvedAudience) {
        for (RPlayer player : resolvedAudience) {
            if (isVisibleTo(player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Refreshes the set of current viewers based on the resolved audience.
     * <p>
     * Sponge visuals are real world entities / server-side particle emissions,
     * so no per-viewer spawn/destroy is required - this only updates the
     * distance-filtered viewer set used for lifecycle and emission decisions.
     *
     * @param resolvedAudience the resolved audience
     */
    public final void refreshViewers(@NotNull Collection<RPlayer> resolvedAudience) {
        if (!shown) return;
        Set<UUID> desired = ConcurrentHashMap.newKeySet();
        for (RPlayer player : resolvedAudience) {
            if (tryUnwrap(player) != null && isVisibleTo(player)) {
                desired.add(player.uuid());
            }
        }
        currentViewers.retainAll(desired);
        currentViewers.addAll(desired);
    }

    /**
     * Returns the location used for distance-based visibility checks.
     * Subclasses may override to provide a specific center point.
     *
     * @return the visibility center, or {@code null} for unlimited distance
     */
    protected @Nullable RLocation visibilityCenter() {
        return null;
    }

    /**
     * Checks whether a player can see this visual based on distance.
     *
     * @param player the player
     * @return {@code true} if visible
     */
    protected boolean isVisibleTo(@NotNull RPlayer player) {
        if (tryUnwrap(player) == null) return false;
        RLocation center = visibilityCenter();
        return center == null || canSeeLocation(player, center, config.viewDistance());
    }

    /**
     * Checks whether a player can see a specific location within the given max distance.
     *
     * @param player      the player
     * @param location    the location
     * @param maxDistance the maximum view distance, negative for unlimited
     * @return {@code true} if within distance
     */
    protected final boolean canSeeLocation(@NotNull RPlayer player, @NotNull RLocation location, double maxDistance) {
        RLocation playerLocation = player.asEntity().flatMap(e -> e.location()).orElse(null);
        if (playerLocation == null) return false;
        if (!playerLocation.world().identifier().equals(location.world().identifier())) return false;
        if (maxDistance < 0) return true;
        double dx = playerLocation.x() - location.x();
        double dy = playerLocation.y() - location.y();
        double dz = playerLocation.z() - location.z();
        return dx * dx + dy * dy + dz * dz <= maxDistance * maxDistance;
    }

    protected final @NotNull Set<UUID> currentViewerIds() {
        return Set.copyOf(currentViewers);
    }

    // ── Entity lifecycle hooks ──────────────────────────────────────────────

    /**
     * Spawns the world entities (or starts per-tick emission) for this visual.
     * Called from {@link #show()}.
     */
    protected abstract void spawnEntities();

    /**
     * Despawns the world entities (or stops per-tick emission) for this visual.
     * Called from {@link #hide()}.
     */
    protected abstract void despawnEntities();

    /**
     * Re-validates that the visual's world entities are still present and
     * re-spawns them if they were removed (e.g. by a chunk unload). Called on
     * every manager tick while the visual is shown. The default implementation
     * is a no-op for transient visuals; entity-based visuals rely on
     * {@link #spawnEntities()} being idempotent.
     */
    protected void ensureEntitiesSpawned() {
        // no-op for transient visuals (e.g. particles)
    }

    // ── Sponge API helpers ──────────────────────────────────────────────────

    /**
     * Resolves a Rapunzel world reference to a Sponge {@link ServerWorld}.
     *
     * @param worldRef the world reference
     * @return the server world, or {@code null} if it is not loaded
     */
    protected final @Nullable ServerWorld serverWorld(@NotNull RWorldRef worldRef) {
        ResourceKey key = worldRef.key() != null
            ? ResourceKey.of(worldRef.key().namespace(), worldRef.key().path())
            : ResourceKey.of("minecraft", worldRef.name());
        return Sponge.server().worldManager().world(key).orElse(null);
    }

    /**
     * Resolves a Rapunzel location to a Sponge {@link org.spongepowered.api.world.server.ServerLocation}.
     *
     * @param location the Rapunzel location
     * @return the server location, or {@code null} if the world is not loaded
     */
    protected final @Nullable org.spongepowered.api.world.server.ServerLocation serverLocation(@NotNull RLocation location) {
        ServerWorld world = serverWorld(location.world());
        return world != null ? world.location(location.x(), location.y(), location.z()) : null;
    }

    /**
     * Resolves the default {@link BlockState} for a Rapunzel block type.
     *
     * @param blockType the block type
     * @return the default block state, or air if the type cannot be resolved
     */
    protected final @NotNull BlockState resolveBlockState(@NotNull RBlockType blockType) {
        BlockType nativeType = blockType.tryHandle(BlockType.class).orElse(null);
        return nativeType != null ? nativeType.defaultState() : BlockTypes.AIR.get().defaultState();
    }

    /**
     * Creates, configures, and spawns a {@link BlockDisplay} entity in the world.
     * The spawned entity is automatically tracked and despawned on hide/remove.
     *
     * @param world     the target world
     * @param position  the world position of the entity
     * @param block     the block type to display
     * @param transform the display transformation (translation/rotation/scale)
     * @param glow      whether the display should glow
     * @return the spawned display entity
     */
    protected final @NotNull BlockDisplay spawnBlockDisplay(
        @NotNull ServerWorld world,
        @NotNull Vector3d position,
        @NotNull RBlockType block,
        @NotNull DisplayTransform transform,
        boolean glow
    ) {
        BlockDisplay display = world.createEntity(
            org.spongepowered.api.entity.EntityTypes.BLOCK_DISPLAY.get(),
            position
        );
        display.offer(Keys.BLOCK_STATE, resolveBlockState(block));
        display.offer(Keys.TRANSFORM, toSpongeTransform(transform));
        display.offer(Keys.IS_GLOWING, glow);
        world.spawnEntity(display);
        trackEntity(display);
        return display;
    }

    /**
     * Removes an entity from the world if it is still present.
     *
     * @param entity the entity to remove
     */
    protected static void despawn(@Nullable org.spongepowered.api.entity.Entity entity) {
        if (entity != null && !entity.isRemoved()) {
            entity.remove();
        }
    }

    /**
     * Converts a Rapunzel {@link DisplayTransform} to a Sponge {@link Transform}.
     * <p>
     * Sponge models the display transformation as translation + euler rotation +
     * scale; the left rotation quaternion is converted to euler angles and the
     * right rotation is dropped (an approximation of the vanilla behavior).
     *
     * @param transform the Rapunzel display transform
     * @return the Sponge transform
     */
    protected static @NotNull Transform toSpongeTransform(@NotNull DisplayTransform transform) {
        Vector3f translation = transform.translation();
        Vector3f scale = transform.scale();
        Quaternionf rotation = transform.leftRotation();
        Vector3d euler = rotation != null ? toEulerAngles(rotation) : Vector3d.ZERO;
        return Transform.of(
            new Vector3d(translation.x(), translation.y(), translation.z()),
            euler,
            new Vector3d(scale.x(), scale.y(), scale.z())
        );
    }

    /**
     * Converts a Rapunzel {@link Quaternionf} to euler angles (pitch, yaw, roll)
     * in radians, matching the XYZ convention used by Sponge's {@link Transform}.
     *
     * @param rotation the quaternion
     * @return the euler angles
     */
    private static @NotNull Vector3d toEulerAngles(@NotNull Quaternionf rotation) {
        double x = rotation.x();
        double y = rotation.y();
        double z = rotation.z();
        double w = rotation.w();
        // Standard quaternion -> Tait-Bryan XYZ (extrinsic) conversion.
        double roll = Math.atan2(2.0 * (w * x + y * z), 1.0 - 2.0 * (x * x + y * y));
        double sinPitch = 2.0 * (w * y - z * x);
        double pitch = Math.abs(sinPitch) >= 1.0 ? Math.copySign(Math.PI / 2.0, sinPitch) : Math.asin(sinPitch);
        double yaw = Math.atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (y * y + z * z));
        return new Vector3d(roll, pitch, yaw);
    }

    /**
     * Maps a Rapunzel RGB value to a Sponge {@link Color}.
     *
     * @param rgb the RGB value (0xRRGGBB)
     * @return the Sponge color
     */
    protected static @NotNull Color toSpongeColor(int rgb) {
        return Color.ofRgb(rgb);
    }

    /**
     * Tries to unwrap an RPlayer to a native Sponge {@link ServerPlayer}.
     *
     * @param player the Rapunzel player
     * @return the native server player, or {@code null}
     */
    @SuppressWarnings("unchecked")
    protected final @Nullable ServerPlayer tryUnwrap(@NotNull RPlayer player) {
        if (!(player instanceof RNativeHandle<?> nativeHandle)) return null;
        Object handle = nativeHandle.handle();
        return handle instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    /**
     * Unwraps an RPlayer to its native Sponge {@link ServerPlayer}.
     *
     * @param player the Rapunzel player
     * @return the native server player
     */
    @SuppressWarnings("unchecked")
    protected final @NotNull ServerPlayer unwrap(@NotNull RPlayer player) {
        return ((RNativeHandle<ServerPlayer>) player).handle();
    }

    /**
     * Tracks a spawned entity so it is despawned on hide/remove.
     *
     * @param entity the entity to track
     */
    protected final void trackEntity(@NotNull org.spongepowered.api.entity.Entity entity) {
        trackedEntities.add(entity);
    }

    /**
     * Despawns all tracked entities and clears the tracking set.
     */
    protected final void despawnTracked() {
        for (org.spongepowered.api.entity.Entity entity : trackedEntities) {
            despawn(entity);
        }
        trackedEntities.clear();
    }
}
