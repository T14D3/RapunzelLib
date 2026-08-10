package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.EntityDeathPost;
import de.t14d3.rapunzellib.events.entity.EntityDeathPre;
import de.t14d3.rapunzellib.events.shared.SharedEntityDeathHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shared Fabric/NeoForge bridge for {@link EntityDeathPre} / {@link EntityDeathPost}
 * on non-player living entities, injecting into {@code LivingEntity.die(DamageSource)}.
 *
 * <p>Player deaths do NOT flow through this method in 26.1.2: {@code ServerPlayer}
 * overrides {@code die} without calling {@code super.die} (the death refactor
 * moved the whole player death path into the override). Player deaths are
 * covered by {@link PlayerDeathMixin}.</p>
 *
 * <p>Pre at HEAD is cancellable - denying cancels the {@code die} call and the
 * entity does not die. Post at RETURN fires only after the death processing
 * completed; the {@code dead} flag guard (plus the in-progress marker) skips
 * the early returns (already-dead entry, NeoForge {@code LivingDeathEvent}
 * cancelled by another mod), mirroring Paper's "no Post when the Pre was
 * denied" semantics (a cancelled HEAD never reaches RETURN at all).</p>
 */
@Mixin(LivingEntity.class)
public abstract class EntityDeathMixin {

    /** Vanilla {@code LivingEntity.dead} - only set once die() commits the death. */
    @Shadow
    protected boolean dead;

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void onEntityDeathPre(DamageSource source, CallbackInfo ci) {
        // Mirror the vanilla guard: die() no-ops for removed/already-dead
        // entities - no event for a death that never happens.
        if (((LivingEntity) (Object) this).isRemoved() || dead) return;
        // ServerPlayer.die overrides this method without calling super in
        // 26.1.2, but stay defensive against version drift: players are the
        // PlayerDeathMixin's job.
        if ((Object) this instanceof ServerPlayer) return;

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPreListeners(EntityDeathPre.class)) return;

        if (SharedEntityDeathHooks.dispatchDeathPre(bus, (LivingEntity) (Object) this, source)) {
            ci.cancel();
        }
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void onEntityDeathPost(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer) return;

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPostListeners(EntityDeathPost.class)) return;

        // `dead` is only true when LivingEntity.die committed the death
        // (set right after the cancellation point); early returns (already
        // dead, cancelled LivingDeathEvent) keep it false.
        SharedEntityDeathHooks.dispatchDeathPost(bus, (LivingEntity) (Object) this, source, dead);
    }
}
