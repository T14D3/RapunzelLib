package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryHandles;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class SpongeWorld extends RNativeHandle<ServerWorld> implements RWorld {
    SpongeWorld(ServerWorld handle, @NotNull SpongeAttachmentService attachmentService) {
        super(PlatformId.SPONGE, Objects.requireNonNull(handle, "handle"), Objects.requireNonNull(attachmentService, "attachmentService").forWorld(handle));
    }

    void updateHandle(ServerWorld newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public @NotNull RWorldRef ref() {
        return SpongeEntitySemantics.worldRef(handle());
    }

    @Override
    public @NotNull Optional<UUID> uuid() {
        return Optional.of(handle().uniqueId());
    }

    @Override
    public boolean canSpawnEntities() {
        return true;
    }

    @Override
    public @NotNull Optional<REntity> spawn(@NotNull RRegistryRef<REntityType> type, @NotNull RLocation location) {
        ServerWorld targetWorld = SpongeEntitySemantics.resolveWorld(location.world(), handle());
        EntityType<?> entityType = RRegistryHandles.find(type, EntityType.class)
            .or(() -> org.spongepowered.api.Sponge.server().registry(org.spongepowered.api.registry.RegistryTypes.ENTITY_TYPE)
                .findValue(org.spongepowered.api.ResourceKey.resolve(type.key().asString())))
            .orElse(null);
        if (entityType == null) {
            return Optional.empty();
        }

        Entity entity = targetWorld.createEntity(entityType, new org.spongepowered.math.vector.Vector3d(location.x(), location.y(), location.z()));
        if (entity == null) {
            return Optional.empty();
        }
        SpongeEntitySemantics.applyRotation(entity, location);
        if (!targetWorld.spawnEntity(entity)) {
            return Optional.empty();
        }
        return Rapunzel.context().entities().wrap(entity);
    }
}
