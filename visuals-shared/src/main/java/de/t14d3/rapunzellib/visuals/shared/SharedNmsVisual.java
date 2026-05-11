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

public abstract class SharedNmsVisual<C extends VisualConfig> implements Visual<C> {
    protected final VisualId id;
    protected final C config;
    protected final VisualAudience audience;
    protected volatile boolean shown = false;
    protected final Set<UUID> currentViewers = ConcurrentHashMap.newKeySet();
    private final VisualManager manager;

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

    public final void onViewerQuit(@NotNull UUID uuid) {
        currentViewers.remove(uuid);
    }

    public final boolean hasCurrentViewers() {
        return !currentViewers.isEmpty();
    }

    public final boolean hasEligibleViewers(@NotNull Collection<RPlayer> resolvedAudience) {
        for (RPlayer player : resolvedAudience) {
            if (isVisibleTo(player)) {
                return true;
            }
        }
        return false;
    }

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

    protected @Nullable RLocation visibilityCenter() {
        return null;
    }

    protected boolean isVisibleTo(@NotNull RPlayer player) {
        if (tryUnwrap(player) == null) return false;
        RLocation center = visibilityCenter();
        return center == null || canSeeLocation(player, center, config.viewDistance());
    }

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

    protected final @NotNull Set<UUID> currentViewerIds() {
        return Set.copyOf(currentViewers);
    }

    protected abstract void spawnFor(@NotNull ServerPlayer player);

    protected abstract void destroyFor(@NotNull ServerPlayer player);

    protected final void send(@NotNull ServerPlayer player, @NotNull Packet<?> packet) {
        player.connection.send(packet);
    }

    @SuppressWarnings("unchecked")
    protected final @NotNull ServerPlayer unwrap(@NotNull RPlayer player) {
        return ((RNativeHandle<ServerPlayer>) player).handle();
    }

    @SuppressWarnings("unchecked")
    protected final @Nullable ServerPlayer tryUnwrap(@NotNull RPlayer player) {
        if (!(player instanceof RNativeHandle<?> nativeHandle)) return null;
        Object handle = nativeHandle.handle();
        return handle instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }
}
