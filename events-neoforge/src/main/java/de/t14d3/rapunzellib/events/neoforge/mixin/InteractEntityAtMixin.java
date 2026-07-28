package de.t14d3.rapunzellib.events.neoforge.mixin;

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

/**
 * DISABLED - This mixin would double-dispatch {@code InteractEntityPost} with {@link InteractEntityMixin}.
 *
 * <p>{@link Player#interactOn} internally calls {@link Entity#interact}, so the player-side
 * injection ({@code Player#interactOn} @RETURN) already captures every entity interaction.
 * This entity-side injection is kept as a compiling (but dormant) mixin for reference.
 *
 * <p>To re-enable, uncomment the {@code @Inject} annotation and method body.</p>
 */
@Mixin(Entity.class)
public abstract class InteractEntityAtMixin {
    // DISABLED: Player.interactOn injection (InteractEntityMixin) already captures this.
    // @Inject(method = "interact", at = @At("RETURN"))
    private void onInteractEntityAtPost(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        // GameEventBus bus = SharedMixinEventsBridge.bus();
        // if (bus == null) return;
        // if (!(player instanceof ServerPlayer serverPlayer)) return;
        // SharedEntityInteractionHooks.dispatchInteractPost(bus, serverPlayer, (Entity) (Object) this, false);
    }
}