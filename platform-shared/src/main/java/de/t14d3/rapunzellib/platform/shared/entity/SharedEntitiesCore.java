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

public abstract class SharedEntitiesCore<E extends REntity> implements Entities {
    protected final MinecraftServer server;
    protected final Function<ServerPlayer, RServerPlayer> playerWrapper;
    private final ConcurrentHashMap<UUID, E> cache = new ConcurrentHashMap<>();

    protected SharedEntitiesCore(@NotNull MinecraftServer server, @NotNull Function<ServerPlayer, RServerPlayer> playerWrapper) {
        this.server = Objects.requireNonNull(server, "server");
        this.playerWrapper = Objects.requireNonNull(playerWrapper, "playerWrapper");
    }

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

    @Override
    public final @NotNull Optional<REntity> wrap(@NotNull Object nativeEntity) {
        Objects.requireNonNull(nativeEntity, "nativeEntity");
        return adaptNativeEntity(nativeEntity).map(this::wrapEntity).map(REntity.class::cast);
    }

    public final @NotNull Optional<E> wrapNative(@NotNull Entity entity) {
        return Optional.of(wrapEntity(entity));
    }

    protected @NotNull Optional<? extends Entity> adaptNativeEntity(@NotNull Object nativeEntity) {
        return nativeEntity instanceof Entity entity ? Optional.of(entity) : Optional.empty();
    }

    protected abstract @NotNull E createEntity(@NotNull Entity entity);

    protected abstract void updateEntity(@NotNull E existingEntity, @NotNull Entity entity);

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

    @NotNull
    protected REntity wrapInternal(@NotNull Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return playerWrapper.apply(player);
        }
        return wrapEntity(entity);
    }
}
