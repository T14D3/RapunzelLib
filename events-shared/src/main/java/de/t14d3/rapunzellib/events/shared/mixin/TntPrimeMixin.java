package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.events.world.TntPrimePre;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;

import net.minecraft.core.BlockPos;
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

        // #if VERSION >= 1.21.11
        String worldId = level.dimension().identifier().toString();
        // #else
        String worldId = level.dimension().location().toString();
        // #endif
        RWorld rWorld = Rapunzel.worlds().require(level);
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        RBlock block = Rapunzel.blocks().at(rWorld, rPos);

        TntPrimePre pre = new TntPrimePre(block, "EXPLOSION", null);
        bus.dispatchPre(pre);

        if (pre.isDenied()) {
            ci.cancel();
        }
    }
}
