package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.EntityDeathPost;
import de.t14d3.rapunzellib.events.entity.EntityDeathPre;
import de.t14d3.rapunzellib.events.shared.SharedEntityDeathHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shared Fabric/NeoForge bridge for {@link EntityDeathPre} / {@link EntityDeathPost}
 * for <em>players</em>, injecting into {@code ServerPlayer.die(DamageSource)}.
 *
 * <p>In 26.1.2 (and 1.21.x) {@code ServerPlayer.die} is a full override that
 * never calls {@code super.die} - the player death path (death message
 * broadcast, loot, stats, last-death location) lives entirely in the override.
 * A mixin on {@code LivingEntity.die} alone would silently miss all player
 * deaths, so this second mixin targets the override. Non-player deaths are
 * covered by {@link EntityDeathMixin}.</p>
 *
 * <p>Pre at HEAD is cancellable - denying cancels the {@code die} call and the
 * player does not die. Post at RETURN fires after the death processing
 * completed; the in-progress marker skips the early return taken when a
 * NeoForge {@code LivingDeathEvent} is cancelled by another mod (the player
 * death path has no other early exits). {@code ServerPlayer.die} never sets
 * the {@code dead} flag - the player entity stays alive for the respawn
 * screen - so the marker alone decides for players.</p>
 */
@Mixin(net.minecraft.server.level.ServerPlayer.class)
public abstract class PlayerDeathMixin {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void onPlayerDeathPre(DamageSource source, CallbackInfo ci) {
        // Defensive guard mirroring LivingEntity.die: die() must not fire for
        // an already-removed entity (the override has no guard of its own).
        // The vanilla `dead` flag is never set by the player death path, so it
        // cannot guard here.
        if (((LivingEntity) (Object) this).isRemoved()) return;

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPreListeners(EntityDeathPre.class)) return;

        if (SharedEntityDeathHooks.dispatchDeathPre(bus, (LivingEntity) (Object) this, source)) {
            ci.cancel();
        }
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void onPlayerDeathPost(DamageSource source, CallbackInfo ci) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPostListeners(EntityDeathPost.class)) return;

        // The in-progress marker alone decides for players: ServerPlayer.die
        // never sets the vanilla `dead` flag (the player entity stays alive
        // for the respawn screen).
        SharedEntityDeathHooks.dispatchDeathPost(bus, (LivingEntity) (Object) this, source, false);
    }
}
