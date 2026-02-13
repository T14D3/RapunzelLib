package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public abstract class SharedWorldBase extends RNativeHandle<ServerLevel> implements RWorld {
    private final SharedWorldHooks worldHooks;

    protected SharedWorldBase(@NotNull PlatformId platformId, @NotNull ServerLevel world) {
        this(platformId, world, RAttachmentContainer.lazyMutable(), SharedWorldHooks.unsupported());
    }

    protected SharedWorldBase(
        @NotNull PlatformId platformId,
        @NotNull ServerLevel world,
        @NotNull RAttachmentContainer attachments,
        @NotNull SharedWorldHooks worldHooks
    ) {
        super(platformId, Objects.requireNonNull(world, "world"), Objects.requireNonNull(attachments, "attachments"));
        this.worldHooks = Objects.requireNonNull(worldHooks, "worldHooks");
    }

    @Override
    public final @NotNull RWorldRef ref() {
        return worldHooks.worldRef(handle());
    }

    @Override
    public final @NotNull Optional<UUID> uuid() {
        return worldHooks.worldUuid(handle());
    }

    @Override
    public final boolean canSpawnEntities() {
        return true;
    }

    @Override
    public final @NotNull Optional<REntity> spawn(@NotNull RRegistryRef<REntityType> type, @NotNull RLocation location) {
        return SharedEntityOperations.spawn(handle(), type, location, worldHooks);
    }
}
