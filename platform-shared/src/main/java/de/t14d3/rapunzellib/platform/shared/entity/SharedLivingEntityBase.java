package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public abstract class SharedLivingEntityBase extends SharedEntityBase implements RLivingEntity {
    protected SharedLivingEntityBase(
        @NotNull PlatformId platformId,
        @NotNull LivingEntity handle,
        @NotNull Function<ServerLevel, ? extends RWorld> worldFactory
    ) {
        super(platformId, handle, worldFactory);
    }

    protected SharedLivingEntityBase(
        @NotNull PlatformId platformId,
        @NotNull LivingEntity handle,
        @NotNull RAttachmentContainer attachments,
        @NotNull SharedWorldHooks worldHooks
    ) {
        super(platformId, handle, attachments, worldHooks);
    }

    @Override
    public final double health() {
        return SharedLivingEntitySupport.health(handle(LivingEntity.class));
    }

    @Override
    public final double maxHealth() {
        return SharedLivingEntitySupport.maxHealth(handle(LivingEntity.class));
    }

    @Override
    public final int remainingAir() {
        return SharedLivingEntitySupport.remainingAir(handle(LivingEntity.class));
    }

    @Override
    public final int maxAir() {
        return SharedLivingEntitySupport.maxAir(handle(LivingEntity.class));
    }

    @Override
    public final boolean isAlive() {
        return SharedLivingEntitySupport.isAlive(handle(LivingEntity.class));
    }

    @Override
    public final boolean canDamage() {
        return true;
    }

    @Override
    public final boolean damage(double amount) {
        return SharedEntityOperations.damage(handle(LivingEntity.class), amount);
    }

    @Override
    public final boolean canHeal() {
        return true;
    }

    @Override
    public final boolean heal(double amount) {
        return SharedEntityOperations.heal(handle(LivingEntity.class), amount);
    }
}
