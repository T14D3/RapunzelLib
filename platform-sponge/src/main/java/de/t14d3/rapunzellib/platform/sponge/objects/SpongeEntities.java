package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.common.objects.CachedWrapperStore;
import de.t14d3.rapunzellib.objects.Entities;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Optional;
import java.util.UUID;

public final class SpongeEntities extends CachedWrapperStore<UUID, Entity, SpongeEntity> implements Entities {
    private final SpongePlayers players;
    private final SpongeAttachmentService attachmentService;
    private final SpongeWorlds worlds;

    public SpongeEntities(@NotNull SpongePlayers players, @NotNull SpongeAttachmentService attachmentService, @NotNull SpongeWorlds worlds) {
        this.players = players;
        this.attachmentService = attachmentService;
        this.worlds = worlds;
    }

    @Override
    public @NotNull Optional<REntity> get(@NotNull UUID uuid) {
        if (uuid == null || !Sponge.isServerAvailable()) {
            return Optional.empty();
        }

        Optional<REntity> player = players.getServer(uuid).map(entity -> (REntity) entity);
        if (player.isPresent()) {
            return player;
        }

        for (ServerWorld world : Sponge.server().worldManager().worlds()) {
            Optional<Entity> entity = world.entity(uuid);
            if (entity.isPresent()) {
                return Optional.of(wrapInternal(entity.get()));
            }
        }
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<REntity> wrap(@NotNull Object nativeEntity) {
        if (nativeEntity instanceof ServerPlayer player) {
            return players.wrap(player).map(entity -> (REntity) entity);
        }
        if (!(nativeEntity instanceof Entity entity)) {
            return Optional.empty();
        }
        return Optional.of(wrapInternal(entity));
    }

    @Override
    protected @NotNull SpongeEntity createWrapper(@NotNull Entity nativeHandle) {
        if (nativeHandle instanceof Living living) {
            return new SpongeLivingEntity(living, attachmentService, worlds);
        }
        return new SpongeEntity(nativeHandle, attachmentService, worlds);
    }

    @Override
    protected void updateWrapper(@NotNull SpongeEntity existingWrapper, @NotNull Entity nativeHandle) {
        existingWrapper.updateHandle(nativeHandle);
    }

    private @NotNull REntity wrapInternal(@NotNull Entity nativeEntity) {
        return wrapCached(nativeEntity.uniqueId(), nativeEntity);
    }
}
