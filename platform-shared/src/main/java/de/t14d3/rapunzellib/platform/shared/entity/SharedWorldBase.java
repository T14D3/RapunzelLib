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

/**
 * Abstract base implementation of {@link RWorld}, wrapping a Minecraft {@link ServerLevel}.
 * <p>
 * Provides world reference, UUID derivation, and entity spawning capabilities,
 * delegating to {@link SharedEntityOperations} for spawning and {@link SharedWorldHooks}
 * for reference management.
 * </p>
 */
public abstract class SharedWorldBase extends RNativeHandle<ServerLevel> implements RWorld {
    private final SharedWorldHooks worldHooks;

    /**
     * Constructs a new world base with a lazy-mutable attachment container and unsupported world hooks.
     *
     * @param platformId the platform identifier
     * @param world      the native ServerLevel to wrap
     */
    protected SharedWorldBase(@NotNull PlatformId platformId, @NotNull ServerLevel world) {
        this(platformId, world, RAttachmentContainer.lazyMutable(), SharedWorldHooks.unsupported());
    }

    /**
     * Constructs a new world base with explicit attachments and world hooks.
     *
     * @param platformId   the platform identifier
     * @param world        the native ServerLevel to wrap
     * @param attachments  the attachment container for this world
     * @param worldHooks   shared world creation and resolution hooks
     */
    protected SharedWorldBase(
        @NotNull PlatformId platformId,
        @NotNull ServerLevel world,
        @NotNull RAttachmentContainer attachments,
        @NotNull SharedWorldHooks worldHooks
    ) {
        super(platformId, Objects.requireNonNull(world, "world"), Objects.requireNonNull(attachments, "attachments"));
        this.worldHooks = Objects.requireNonNull(worldHooks, "worldHooks");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull RWorldRef ref() {
        return worldHooks.worldRef(handle());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull Optional<UUID> uuid() {
        return worldHooks.worldUuid(handle());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean canSpawnEntities() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull Optional<REntity> spawn(@NotNull RRegistryRef<REntityType> type, @NotNull RLocation location) {
        return SharedEntityOperations.spawn(handle(), type, location, worldHooks);
    }
}
