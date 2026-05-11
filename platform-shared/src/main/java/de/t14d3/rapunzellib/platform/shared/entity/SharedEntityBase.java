package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.nbt.shared.SharedAdventureComponentCodec;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public abstract class SharedEntityBase extends RNativeHandle<Entity> implements REntity {
    private final SharedWorldHooks worldHooks;

    protected SharedEntityBase(
        @NotNull PlatformId platformId,
        @NotNull Entity handle,
        @NotNull Function<ServerLevel, ? extends RWorld> worldFactory
    ) {
        this(platformId, handle, RAttachmentContainer.lazyMutable(), SharedWorldHooks.of(worldFactory));
    }

    protected SharedEntityBase(
        @NotNull PlatformId platformId,
        @NotNull Entity handle,
        @NotNull RAttachmentContainer attachments,
        @NotNull SharedWorldHooks worldHooks
    ) {
        super(platformId, Objects.requireNonNull(handle, "handle"), Objects.requireNonNull(attachments, "attachments"));
        this.worldHooks = Objects.requireNonNull(worldHooks, "worldHooks");
    }

    public void updateHandle(@NotNull Entity newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public final @NotNull UUID uuid() {
        return handle().getUUID();
    }

    @Override
    public final @NotNull RRegistryRef<REntityType> typeRef() {
        return REntityType.ref(BuiltInRegistries.ENTITY_TYPE.getKey(handle().getType()).toString());
    }

    @Override
    public final @NotNull Optional<RWorld> world() {
        ServerLevel level = (ServerLevel) handle().level();
        return Optional.of(worldHooks.createWorld(level));
    }

    @Override
    public final @NotNull Optional<RLocation> location() {
        Entity entity = handle();
        ServerLevel level = (ServerLevel) entity.level();
        return Optional.of(new RLocation(
            worldHooks.worldRef(level),
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            entity.getYRot(),
            entity.getXRot()
        ));
    }

    @Override
    public boolean canTeleport() {
        return true;
    }

    @Override
    public boolean teleport(@NotNull RLocation location) {
        return SharedEntityOperations.teleport(handle(), location, worldHooks);
    }

    @Override
    public @NotNull Optional<String> getName() {
        Component customName = handle().getCustomName();
        if (customName == null) {
            return Optional.empty();
        }
        return Optional.of(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(SharedAdventureComponentCodec.toAdventure(customName)));
    }

    @Override
    public void setName(@NotNull String name) {
        handle().setCustomName(net.minecraft.network.chat.Component.literal(name));
    }

    @Override
    public @NotNull Optional<net.kyori.adventure.text.Component> getDisplayName() {
        Component customName = handle().getCustomName();
        if (customName == null) {
            return Optional.empty();
        }
        return Optional.of(SharedAdventureComponentCodec.toAdventure(customName));
    }

    @Override
    public void setDisplayName(@NotNull net.kyori.adventure.text.Component displayName) {
        handle().setCustomName(SharedAdventureComponentCodec.toNative(displayName));
    }

    @Override
    public boolean remove() {
        handle().remove(Entity.RemovalReason.DISCARDED);
        return true;
    }

    @Override
    public boolean isRemoved() {
        return handle().isRemoved();
    }
}
