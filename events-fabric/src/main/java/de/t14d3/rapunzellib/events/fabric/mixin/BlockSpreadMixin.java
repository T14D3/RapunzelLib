package de.t14d3.rapunzellib.events.fabric.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.shared.mixin.SharedBlockMixinHooks;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to capture block spread events (fire, mushrooms, vines, etc.).
 * Dispatches BlockSpreadPre (cancellable).
 */
@Mixin(Level.class)
public class BlockSpreadMixin {
    private static final String SET_BLOCK_WITH_RECURSION =
        "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z";

    @Inject(method = SET_BLOCK_WITH_RECURSION, at = @At("HEAD"), cancellable = true)
    private void onBlockSpreadPre(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        Level level = (Level) (Object) this;
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (SharedBlockMixinHooks.dispatchBlockSpreadPre(bus, serverLevel, pos, state)) {
            cir.setReturnValue(false);
        }
    }
}
