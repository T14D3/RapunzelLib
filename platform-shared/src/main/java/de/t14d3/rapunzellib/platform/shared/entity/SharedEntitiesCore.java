package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.objects.Entities;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Abstract base implementation of {@link Entities} that provides entity lookup and wrapping.
 * <p>
 * Maintains a {@link ConcurrentHashMap}-backed cache of wrapped entities keyed by UUID.
 * Players are always handled separately via a dedicated {@code playerWrapper} function.
 * </p>
 *
 * @param <E> the concrete entity wrapper type managed by this implementation
 */
public abstract class SharedEntitiesCore<E extends REntity> implements Entities {
    /** The Minecraft server instance used for entity lookups. */
    protected final MinecraftServer server;
    /** Factory for wrapping native {@link ServerPlayer} instances into {@link RServerPlayer}. */
    protected final Function<ServerPlayer, RServerPlayer> playerWrapper;
    private final ConcurrentHashMap<UUID, E> cache = new ConcurrentHashMap<>();

    /**
     * Constructs a new entity core.
     *
     * @param server        the Minecraft server instance
     * @param playerWrapper factory for wrapping ServerPlayer instances
     */
    protected SharedEntitiesCore(@NotNull MinecraftServer server, @NotNull Function<ServerPlayer, RServerPlayer> playerWrapper) {
        this.server = Objects.requireNonNull(server, "server");
        this.playerWrapper = Objects.requireNonNull(playerWrapper, "playerWrapper");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<REntity> get(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return Optional.of(playerWrapper.apply(player));
        }

        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                return Optional.of(wrapInternal(entity));
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull Optional<REntity> wrap(@NotNull Object nativeEntity) {
        Objects.requireNonNull(nativeEntity, "nativeEntity");
        return adaptNativeEntity(nativeEntity).map(this::wrapEntity).map(REntity.class::cast);
    }

    /**
     * Wraps a native Minecraft {@link Entity} into an {@link Optional} containing the managed wrapper type.
     *
     * @param entity the native entity to wrap
     * @return an Optional containing the wrapped entity
     */
    public final @NotNull Optional<E> wrapNative(@NotNull Entity entity) {
        return Optional.of(wrapEntity(entity));
    }

    /**
     * Attempts to adapt a generic native object into a Minecraft {@link Entity}.
     *
     * @param nativeEntity the object to adapt
     * @return an Optional containing the adapted Entity, or empty if not adaptable
     */
    protected @NotNull Optional<? extends Entity> adaptNativeEntity(@NotNull Object nativeEntity) {
        return nativeEntity instanceof Entity entity ? Optional.of(entity) : Optional.empty();
    }

    /**
     * Creates a new entity wrapper for the given native entity.
     *
     * @param entity the native entity to wrap
     * @return the new wrapper instance
     */
    protected abstract @NotNull E createEntity(@NotNull Entity entity);

    /**
     * Updates an existing entity wrapper with fresh data from the given native entity.
     *
     * @param existingEntity the existing wrapper to update
     * @param entity         the native entity providing updated state
     */
    protected abstract void updateEntity(@NotNull E existingEntity, @NotNull Entity entity);

    /**
     * Wraps a native entity, returning either a player wrapper or a cached entity wrapper.
     *
     * @param entity the native entity
     * @return the wrapped entity
     */
    protected final @NotNull E wrapEntity(@NotNull Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return (E) playerWrapper.apply(player);
        }
        return cache.compute(entity.getUUID(), (uuid, existing) -> {
            if (existing == null) return createEntity(entity);
            updateEntity(existing, entity);
            return existing;
        });
    }

    /**
     * Internal wrapping helper that also handles players via the player wrapper.
     *
     * @param entity the native entity to wrap
     * @return the wrapped REntity
     */
    @NotNull
    protected REntity wrapInternal(@NotNull Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return playerWrapper.apply(player);
        }
        return wrapEntity(entity);
    }
}
