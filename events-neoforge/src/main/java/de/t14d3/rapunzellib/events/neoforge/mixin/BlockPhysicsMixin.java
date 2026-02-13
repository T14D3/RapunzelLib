package de.t14d3.rapunzellib.events.neoforge.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.shared.mixin.SharedBlockMixinHooks;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to capture block physics events (neighbor updates, redstone, fluid flow, gravity).
 * Dispatches BlockPhysicsPre (cancellable) and BlockPhysicsPost.
 */
@Mixin(ServerLevel.class)
public class BlockPhysicsMixin {
    private static final String NEIGHBOR_CHANGED =
        "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;)V";
    private static final String SEND_BLOCK_UPDATED =
        "sendBlockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;I)V";

    @Inject(method = NEIGHBOR_CHANGED, at = @At("HEAD"), cancellable = true)
    private void onNeighborChangedPre(BlockPos pos, Block block, Orientation orientation, CallbackInfo ci) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        ServerLevel level = (ServerLevel) (Object) this;
        if (SharedBlockMixinHooks.dispatchBlockPhysicsNeighborPre(bus, level, pos, block)) {
            ci.cancel();
        }
    }

    @Inject(method = NEIGHBOR_CHANGED, at = @At("RETURN"))
    private void onNeighborChangedPost(BlockPos pos, Block block, Orientation orientation, CallbackInfo ci) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        ServerLevel level = (ServerLevel) (Object) this;
        SharedBlockMixinHooks.dispatchBlockPhysicsNeighborPost(bus, level, pos, block);
    }

    @Inject(method = SEND_BLOCK_UPDATED, at = @At("HEAD"), cancellable = true)
    private void onBlockStateChangePre(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        ServerLevel level = (ServerLevel) (Object) this;
        if (SharedBlockMixinHooks.dispatchBlockPhysicsStateChangePre(bus, level, pos, oldState, newState)) {
            ci.cancel();
        }
    }
}
