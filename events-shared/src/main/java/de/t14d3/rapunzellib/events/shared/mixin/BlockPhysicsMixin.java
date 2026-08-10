package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.shared.mixin.SharedBlockMixinHooks;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures neighbor-physics updates at {@link NeighborUpdater#executeUpdate},
 * the single vanilla funnel every neighbor notification flows through (both
 * the {@code updateNeighborsAt} cascade from {@code setBlock} and direct
 * {@code neighborChanged} dispatches).
 *
 * <p>This is exactly the point where Paper fires {@code BlockPhysicsEvent}
 * (Paper 26.x fires it in its patched 7-arg {@code executeUpdate} overload,
 * which the vanilla 6-arg method delegates into), so Fabric/NeoForge mirror
 * Paper's fire semantics: one event per affected neighbor block, and a
 * cancelled pre-event skips the neighbor's physics reaction entirely -
 * NOT a client notification packet.</p>
 *
 * <p>Dispatches {@code BlockPhysicsPre} (cancellable) and
 * {@code BlockPhysicsPost}.</p>
 *
 * <p>Interface mixin: {@code executeUpdate} is a {@code static} method on the
 * {@link NeighborUpdater} interface itself, so the mixin must be an interface
 * (class mixins cannot target interfaces).</p>
 */
@Mixin(NeighborUpdater.class)
public interface BlockPhysicsMixin {

    @Inject(
        method = "executeUpdate(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;Z)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onBlockPhysicsPre(Level level, BlockState state, BlockPos pos, Block changedBlock, Orientation orientation, boolean movedByPiston, CallbackInfo ci) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (SharedBlockMixinHooks.dispatchBlockPhysicsNeighborPre(bus, serverLevel, pos, changedBlock)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "executeUpdate(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;Z)V",
        at = @At("RETURN")
    )
    private static void onBlockPhysicsPost(Level level, BlockState state, BlockPos pos, Block changedBlock, Orientation orientation, boolean movedByPiston, CallbackInfo ci) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        SharedBlockMixinHooks.dispatchBlockPhysicsNeighborPost(bus, serverLevel, pos, changedBlock);
    }
}
