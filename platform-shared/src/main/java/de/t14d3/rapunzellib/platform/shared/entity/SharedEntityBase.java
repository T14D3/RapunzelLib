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

/**
 * Abstract base implementation of a generic Minecraft {@link Entity} wrapper.
 * <p>
 * Provides shared implementations for identity, location, custom names, teleportation,
 * and removal, delegating to utilities like {@link SharedEntityOperations} and
 * {@link SharedAdventureComponentCodec}.
 * </p>
 */
public abstract class SharedEntityBase extends RNativeHandle<Entity> implements REntity {
    private final SharedWorldHooks worldHooks;

    /**
     * Constructs a new entity base with a lazy-mutable attachment container and a world factory.
     *
     * @param platformId   the platform identifier
     * @param handle       the native Minecraft Entity
     * @param worldFactory a function to create {@link RWorld} wrappers from {@link ServerLevel} instances
     */
    protected SharedEntityBase(
        @NotNull PlatformId platformId,
        @NotNull Entity handle,
        @NotNull Function<ServerLevel, ? extends RWorld> worldFactory
    ) {
        this(platformId, handle, RAttachmentContainer.lazyMutable(), SharedWorldHooks.of(worldFactory));
    }

    /**
     * Constructs a new entity base with explicit attachments and world hooks.
     *
     * @param platformId   the platform identifier
     * @param handle       the native Minecraft Entity
     * @param attachments  the attachment container for this entity
     * @param worldHooks   shared world creation and resolution hooks
     */
    protected SharedEntityBase(
        @NotNull PlatformId platformId,
        @NotNull Entity handle,
        @NotNull RAttachmentContainer attachments,
        @NotNull SharedWorldHooks worldHooks
    ) {
        super(platformId, Objects.requireNonNull(handle, "handle"), Objects.requireNonNull(attachments, "attachments"));
        this.worldHooks = Objects.requireNonNull(worldHooks, "worldHooks");
    }

    /**
     * Replaces the underlying native entity handle, typically used after a respawn or dimension change.
     *
     * @param newHandle the new native Entity handle
     */
    public void updateHandle(@NotNull Entity newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull UUID uuid() {
        return handle().getUUID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull RRegistryRef<REntityType> typeRef() {
        return REntityType.ref(BuiltInRegistries.ENTITY_TYPE.getKey(handle().getType()).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull Optional<RWorld> world() {
        ServerLevel level = (ServerLevel) handle().level();
        return Optional.of(worldHooks.createWorld(level));
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canTeleport() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean teleport(@NotNull RLocation location) {
        return SharedEntityOperations.teleport(handle(), location, worldHooks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<String> getName() {
        Component customName = handle().getCustomName();
        if (customName == null) {
            return Optional.empty();
        }
        return Optional.of(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(SharedAdventureComponentCodec.toAdventure(customName)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setName(@NotNull String name) {
        handle().setCustomName(net.minecraft.network.chat.Component.literal(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<net.kyori.adventure.text.Component> getDisplayName() {
        Component customName = handle().getCustomName();
        if (customName == null) {
            return Optional.empty();
        }
        return Optional.of(SharedAdventureComponentCodec.toAdventure(customName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDisplayName(@NotNull net.kyori.adventure.text.Component displayName) {
        handle().setCustomName(SharedAdventureComponentCodec.toNative(displayName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove() {
        handle().remove(Entity.RemovalReason.DISCARDED);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemoved() {
        return handle().isRemoved();
    }
}
