package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.shared.SharedEntityInteractionHooks;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class InteractEntityMixin {
    /**
     * Single source of truth for {@link InteractEntityPost} on Fabric: fired at
     * {@code Player.interactOn} RETURN, i.e. exactly once per physical
     * interaction that actually reaches the vanilla interaction path.
     * <p>
     * The {@code cancelled} flag reflects the real outcome: the Fabric bridge
     * marks the interaction as denied when a pre-listener denies it, and the
     * bridge dispatches the denied post itself (fabric-api cancels
     * {@code ServerGamePacketListenerImpl.handleInteract} BEFORE
     * {@code interactOn} on denial, so this RETURN injection can never fire for
     * a denied interaction). When this injection does run and finds the flag
     * set, the bridge's post already reported the denial - skip to avoid a
     * duplicate.
     */
    @Inject(method = "interactOn", at = @At("RETURN"))
    private void onInteractEntityPost(Entity entity, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (SharedEntityInteractionHooks.consumeInteractDenied()) {
            return;
        }

        SharedEntityInteractionHooks.dispatchInteractPost(bus, player, entity, false);
    }
}
