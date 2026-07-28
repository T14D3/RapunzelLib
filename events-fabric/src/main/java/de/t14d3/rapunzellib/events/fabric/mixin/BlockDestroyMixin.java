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
 * Mixin to capture non-player block destroy events (e.g. {@code /setblock
 * ... destroy}, fluid/lava consumption, explosions). Dispatches
 * BlockDestroyPre (cancellable).
 *
 * <p>Player-initiated breaks are excluded via the thread-local guard in
 * {@link SharedBlockMixinHooks} (set by the Fabric bridge around
 * {@code PlayerBlockBreakEvents.BEFORE/AFTER}). Formation, spread, and
 * transform events are excluded by {@code BlockDestroyUtil}.</p>
 */
@Mixin(Level.class)
public class BlockDestroyMixin {
    private static final String SET_BLOCK_WITH_RECURSION =
        "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z";

    @Inject(
            method = SET_BLOCK_WITH_RECURSION,
            at = @At("HEAD"),
            cancellable = true
    )
    private void onBlockDestroyPre(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        Level level = (Level) (Object) this;
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (SharedBlockMixinHooks.dispatchBlockDestroyPre(bus, serverLevel, pos, state)) {
            cir.setReturnValue(false);
        }
    }
}
