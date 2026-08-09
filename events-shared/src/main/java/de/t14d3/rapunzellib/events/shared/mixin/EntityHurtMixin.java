package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.EntityHurtPre;
import de.t14d3.rapunzellib.events.shared.SharedEntityDamageHooks;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.objects.RKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
// #if VERSION >= 26
// # import net.minecraft.resources.ResourceKey;
// #endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityHurtMixin {
    @Inject(method = "hurtOrSimulate", at = @At("HEAD"), cancellable = true)
    private void onEntityHurtPre(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (!bus.hasPreListeners(EntityHurtPre.class)) return;

        Entity self = (Entity) (Object) this;
        if (!(self.level() instanceof ServerLevel)) return;

        String damageKey;
        // #if VERSION >= 26
        // # damageKey = source.typeHolder().unwrapKey().map(k -> k.identifier().toString()).orElse("unknown");
        // #else
        damageKey = "minecraft:" + source.type().msgId();
        // #endif
        // The direct attacker (arrow, mob, TNT, ...) when entity-sourced;
        // empty for block/environmental damage.
        net.minecraft.world.entity.Entity directDamager = source.getDirectEntity();
        de.t14d3.rapunzellib.objects.REntity damager = directDamager != null && directDamager != self
                ? Rapunzel.entities().require(directDamager)
                : null;
        EntityHurtPre pre = new EntityHurtPre(
            Rapunzel.entities().require(self),
            RKey.of(damageKey),
            damager,
            false
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtOrSimulate", at = @At("RETURN"))
    private void onEntityHurtPost(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        Entity self = (Entity) (Object) this;
        if (!(self.level() instanceof ServerLevel)) return;

        String damageKey;
        // #if VERSION >= 26
        // # damageKey = source.typeHolder().unwrapKey().map(k -> k.identifier().toString()).orElse("unknown");
        // #else
        damageKey = "minecraft:" + source.type().msgId();
        // #endif
        SharedEntityDamageHooks.dispatchHurtOutcome(bus, self, damageKey, !Boolean.TRUE.equals(cir.getReturnValue()));
    }
}