package de.t14d3.rapunzellib.visuals.shared;

import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.visuals.Visual;
import de.t14d3.rapunzellib.visuals.VisualAudience;
import de.t14d3.rapunzellib.visuals.VisualConfig;
import de.t14d3.rapunzellib.visuals.VisualId;
import de.t14d3.rapunzellib.visuals.VisualManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base for all NMS-based visual implementations.
 * <p>
 * Manages the visual lifecycle (show/hide/remove), viewer tracking,
 * and distance-based visibility checks. Subclasses implement
 * {@link #spawnFor(ServerPlayer)} and {@link #destroyFor(ServerPlayer)}
 * to handle platform-specific packet sending.
 *
 * @param <C> the visual config type
 */
public abstract class SharedNmsVisual<C extends VisualConfig> implements Visual<C> {
    /** The visual identifier. */
    protected final VisualId id;
    /** The visual configuration. */
    protected final C config;
    /** The visual audience. */
    protected final VisualAudience audience;
    /** Whether the visual is currently shown. */
    protected volatile boolean shown = false;
    /** Set of UUIDs of current viewers. */
    protected final Set<UUID> currentViewers = ConcurrentHashMap.newKeySet();
    private final VisualManager manager;

    /**
     * Creates a new shared NMS visual.
     *
     * @param id       the visual ID
     * @param config   the visual config
     * @param audience the visual audience
     * @param manager  the visual manager
     */
    protected SharedNmsVisual(
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
        refreshViewers(audience.resolve());
    }

    @Override
    public final void hide() {
        if (!shown) return;
        shown = false;
        Set<UUID> toRemove = Set.copyOf(currentViewers);
        try {
            for (UUID uuid : toRemove) {
                RPlayer player = RPlayer.get(uuid).orElse(null);
                ServerPlayer serverPlayer = player != null ? tryUnwrap(player) : null;
                if (serverPlayer == null) continue;
                try {
                    destroyFor(serverPlayer);
                } catch (Throwable ignored) {
                }
            }
        } finally {
            currentViewers.clear();
        }
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
     * Refreshes the set of viewers based on the resolved audience.
     *
     * @param resolvedAudience the resolved audience
     */
    public final void refreshViewers(@NotNull Collection<RPlayer> resolvedAudience) {
        if (!shown) return;

        Map<UUID, ServerPlayer> desired = new LinkedHashMap<>();
        for (RPlayer player : resolvedAudience) {
            ServerPlayer serverPlayer = tryUnwrap(player);
            if (serverPlayer != null && isVisibleTo(player)) {
                desired.put(player.uuid(), serverPlayer);
            }
        }

        for (UUID uuid : Set.copyOf(currentViewers)) {
            if (!desired.containsKey(uuid)) {
                RPlayer player = RPlayer.get(uuid).orElse(null);
                ServerPlayer serverPlayer = player != null ? tryUnwrap(player) : null;
                if (serverPlayer != null) {
                    destroyFor(serverPlayer);
                }
                currentViewers.remove(uuid);
            }
        }

        for (Map.Entry<UUID, ServerPlayer> entry : desired.entrySet()) {
            if (currentViewers.add(entry.getKey())) {
                spawnFor(entry.getValue());
            }
        }
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
        RLocation playerLocation = player.asEntity().flatMap(REntity::location).orElse(null);
        if (playerLocation == null) return false;
        if (!playerLocation.world().identifier().equals(location.world().identifier())) return false;
        if (maxDistance < 0) return true;
        double dx = playerLocation.x() - location.x();
        double dy = playerLocation.y() - location.y();
        double dz = playerLocation.z() - location.z();
        return dx * dx + dy * dy + dz * dz <= maxDistance * maxDistance;
    }

    /**
     * Returns an immutable copy of the current viewer UUIDs.
     *
     * @return the current viewer IDs
     */
    protected final @NotNull Set<UUID> currentViewerIds() {
        return Set.copyOf(currentViewers);
    }

    /**
     * Spawns the visual for a specific player. Called when a new viewer is added.
     *
     * @param player the server player
     */
    protected abstract void spawnFor(@NotNull ServerPlayer player);

    /**
     * Destroys the visual for a specific player. Called when a viewer is removed.
     *
     * @param player the server player
     */
    protected abstract void destroyFor(@NotNull ServerPlayer player);

    /**
     * Sends a packet to a player.
     *
     * @param player the server player
     * @param packet the packet to send
     */
    protected final void send(@NotNull ServerPlayer player, @NotNull Packet<?> packet) {
        player.connection.send(packet);
    }

    /**
     * Unwraps an RPlayer to a native ServerPlayer.
     *
     * @param player the Rapunzel player
     * @return the native server player
     */
    @SuppressWarnings("unchecked")
    protected final @NotNull ServerPlayer unwrap(@NotNull RPlayer player) {
        return ((RNativeHandle<ServerPlayer>) player).handle();
    }

    /**
     * Tries to unwrap an RPlayer to a native ServerPlayer.
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
}
