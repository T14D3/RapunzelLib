package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPre;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.shared.SharedEntitySpawnHooks;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to capture entity spawn events in ServerLevel.
 * Dispatches EntitySpawnPre (cancellable), EntitySpawnPost, and EntitySpawnSnapshot.
 */
@Mixin(ServerLevel.class)
public class EntitySpawnMixin {

    @Inject(
        method = "addFreshEntity",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onEntitySpawnPre(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        boolean needsPre = bus.hasPreListeners(EntitySpawnPre.class);
        boolean needsPost = bus.hasPostListeners(EntitySpawnPost.class);
        boolean needsAsync = bus.hasAsyncListeners(EntitySpawnSnapshot.class);

        if (!needsPre && !needsPost && !needsAsync) return;

        String reason = "NATURAL"; // Fabric doesn't provide spawn reason directly

        if (SharedEntitySpawnHooks.dispatchSpawnPre(bus, entity, reason, false)) {
            SharedEntitySpawnHooks.dispatchCancelledSpawnSnapshot(bus, entity, reason);
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "addFreshEntity",
        at = @At("RETURN")
    )
    private void onEntitySpawnPost(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        // Only dispatch if entity was actually added (return value is true)
        if (!cir.getReturnValue()) return;

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        boolean needsPost = bus.hasPostListeners(EntitySpawnPost.class);
        boolean needsAsync = bus.hasAsyncListeners(EntitySpawnSnapshot.class);

        if (!needsPost && !needsAsync) return;

        String reason = "NATURAL";

        SharedEntitySpawnHooks.dispatchSpawnOutcome(bus, entity, reason, false);
    }
}
