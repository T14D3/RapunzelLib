package de.t14d3.rapunzellib.events.fabric.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.shared.SharedEntityDamageHooks;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityHurtMixin {
    @Inject(method = "hurtOrSimulate", at = @At("RETURN"))
    private void onEntityHurtPost(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        Entity self = (Entity) (Object) this;
        if (!(self.level() instanceof ServerLevel)) return;

        SharedEntityDamageHooks.dispatchHurtOutcome(bus, self, source.type().msgId(), !Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
