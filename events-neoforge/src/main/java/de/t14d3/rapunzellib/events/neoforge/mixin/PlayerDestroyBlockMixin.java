package de.t14d3.rapunzellib.events.neoforge.mixin;

import de.t14d3.rapunzellib.events.shared.mixin.SharedBlockMixinHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Thread-local guard around player block breaks so that the
 * {@link BlockDestroyMixin} (on {@code Level.setBlock}) does not dispatch
 * a {@code BlockDestroyPre} for the {@code setBlock} call inside the player
 * break flow. The break itself is surfaced via the NeoForge
 * {@code BlockEvent.BreakEvent} callback in {@link NeoForgeGameEventsBridge}.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class PlayerDestroyBlockMixin {

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void onDestroyBlockStart(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        SharedBlockMixinHooks.beginPlayerBreak();
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void onDestroyBlockEnd(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        SharedBlockMixinHooks.endPlayerBreak();
    }
}