package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Abstract base implementation of a living entity (mobs, players, etc.), extending {@link SharedEntityBase}.
 * <p>
 * Delegates health, air supply, damage, and healing operations to
 * {@link SharedLivingEntitySupport} and {@link SharedEntityOperations}.
 * </p>
 */
public abstract class SharedLivingEntityBase extends SharedEntityBase implements RLivingEntity {
    /**
     * Constructs a new living entity base with a world factory.
     *
     * @param platformId   the platform identifier
     * @param handle       the native Minecraft LivingEntity
     * @param worldFactory a function to create {@link RWorld} wrappers from {@link ServerLevel} instances
     */
    protected SharedLivingEntityBase(
        @NotNull PlatformId platformId,
        @NotNull LivingEntity handle,
        @NotNull Function<ServerLevel, ? extends RWorld> worldFactory
    ) {
        super(platformId, handle, worldFactory);
    }

    /**
     * Constructs a new living entity base with explicit attachments and world hooks.
     *
     * @param platformId   the platform identifier
     * @param handle       the native Minecraft LivingEntity
     * @param attachments  the attachment container for this entity
     * @param worldHooks   shared world creation and resolution hooks
     */
    protected SharedLivingEntityBase(
        @NotNull PlatformId platformId,
        @NotNull LivingEntity handle,
        @NotNull RAttachmentContainer attachments,
        @NotNull SharedWorldHooks worldHooks
    ) {
        super(platformId, handle, attachments, worldHooks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final double health() {
        return SharedLivingEntitySupport.health(handle(LivingEntity.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final double maxHealth() {
        return SharedLivingEntitySupport.maxHealth(handle(LivingEntity.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int remainingAir() {
        return SharedLivingEntitySupport.remainingAir(handle(LivingEntity.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int maxAir() {
        return SharedLivingEntitySupport.maxAir(handle(LivingEntity.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isAlive() {
        return SharedLivingEntitySupport.isAlive(handle(LivingEntity.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean canDamage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean damage(double amount) {
        return SharedEntityOperations.damage(handle(LivingEntity.class), amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean canHeal() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean heal(double amount) {
        return SharedEntityOperations.heal(handle(LivingEntity.class), amount);
    }
}
