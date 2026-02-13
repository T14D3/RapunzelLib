package de.t14d3.rapunzellib.events.fabric.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.events.world.TntPrimePre;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.TntBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to capture TNT priming events.
 * Dispatches TntPrimePre (cancellable).
 */
@Mixin(TntBlock.class)
public class TntPrimeMixin {

 /**
  * Inject into wasExploded - called when TNT is exploded by another explosion.
  */
 @Inject(
 method = "wasExploded",
 at = @At("HEAD"),
 cancellable = true
 )
 private void onTntExplodedPre(ServerLevel level, BlockPos pos, net.minecraft.world.level.Explosion explosion, CallbackInfo ci) {
 GameEventBus bus = SharedMixinEventsBridge.bus();
 if (bus == null) return;

 if (!bus.hasPreListeners(TntPrimePre.class)) return;

 String worldId = level.dimension().identifier().toString();
 RWorldRef worldRef = new RWorldRef(worldId, worldId);
 RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
 String blockTypeKey = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();

 TntPrimePre pre = new TntPrimePre(worldRef, rPos, blockTypeKey, "EXPLOSION", null);
 bus.dispatchPre(pre);

 if (pre.isDenied()) {
 ci.cancel();
 }
 }

 /**
  * Inject into prime - called when TNT is primed (ignited).
  * Note: prime is static, so we use @At("INVOKE") on call sites instead.
  * For now, we capture the explosion-based priming via wasExploded.
  */
}
