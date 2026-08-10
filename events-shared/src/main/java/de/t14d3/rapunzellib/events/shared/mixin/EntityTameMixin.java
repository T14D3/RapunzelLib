package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.EntityTamePost;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric-only bridge for {@link EntityTamePost}, injecting into
 * {@code TamableAnimal.tame(Player)} at RETURN.
 *
 * <p>{@code tame} is the tame-success path in 26.1.2 (identical in
 * 1.21.10/1.21.11/26.2): it is only invoked after the tame roll succeeded
 * (from the per-animal feed/interact logic) and applies the owner + tamed
 * flags. A RETURN injection therefore fires exactly when a tame succeeded -
 * the same semantics as Paper's {@code EntityTameEvent} and NeoForge's native
 * {@code AnimalTameEvent} (which fires right before {@code tame} and is
 * cancellable; the NeoForge bridge dispatches only when it was not
 * cancelled). This mixin must NOT be registered on NeoForge - the native
 * event is the single source there.</p>
 *
 * <p>Mirrors the Paper payload: the tamer as a live {@link RPlayer} (only when
 * the tamer is a player) and the tamed entity.</p>
 */
@Mixin(TamableAnimal.class)
public abstract class EntityTameMixin {

    @Inject(method = "tame", at = @At("RETURN"))
    private void onEntityTamePost(Player player, CallbackInfo ci) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPostListeners(EntityTamePost.class)) return;
        // The tamer is a player during the tame interaction; other tamer
        // implementations cannot be resolved to an RPlayer (Paper skips too).
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        TamableAnimal self = (TamableAnimal) (Object) this;
        if (!(self.level() instanceof ServerLevel)) return;

        RPlayer tamer = Rapunzel.players().require(serverPlayer);
        REntity tamed = Rapunzel.entities().require(self);
        if (tamed == null) return;

        bus.dispatchPost(new EntityTamePost(tamer, tamed));
    }
}
