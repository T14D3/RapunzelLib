package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.block.PistonMovePre;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared Fabric/NeoForge bridge for {@link PistonMovePre}.
 *
 * <p>Injects into the vanilla piston move path in 26.1.2 (identical in
 * 1.21.10/1.21.11/26.2): {@code PistonBaseBlock.moveBlocks(Level, BlockPos,
 * Direction, boolean extending)} covers both extension ({@code extending=true},
 * push) and sticky retraction with pullable blocks ({@code extending=false}),
 * and {@code PistonBaseBlock.triggerEvent} covers the retraction cases that
 * never reach {@code moveBlocks} (non-sticky contracts and sticky retracts
 * with nothing to pull), mirroring Paper's {@code BlockPistonExtendEvent} /
 * {@code BlockPistonRetractEvent} firing surface (Paper fires the retract
 * event with an empty block list for those paths).</p>
 *
 * <p>Payload mirrors the Paper bridge: the block list of the structure
 * resolver ({@code toPush} + {@code toDestroy}, the same list Paper feeds the
 * Bukkit event) with one destination per source in the movement direction
 * (extend = piston facing, retract = opposite). Deny = the piston does not
 * move: {@code moveBlocks} returns {@code false} (extension aborts before any
 * block is placed) or {@code triggerEvent} returns {@code false} (no moving
 * block entity is placed, no sound).</p>
 */
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {

    @Shadow
    private boolean isSticky;

    /**
     * Extension (type 0) and sticky-pull retraction flow through here; the
     * moved blocks are resolved exactly like the vanilla method resolves them,
     * so the event fires only for actually pushable structures (Paper fires
     * the piston event at the same point, after {@code resolve()} succeeded).
     */
    @Inject(method = "moveBlocks", at = @At("HEAD"), cancellable = true)
    private void onPistonMovePre(Level level, BlockPos pos, Direction direction, boolean extending,
                                 CallbackInfoReturnable<Boolean> cir) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPreListeners(PistonMovePre.class)) return;
        if (!(level instanceof ServerLevel)) return;

        // Same computation the vanilla method runs right after our HEAD: pure
        // read of the level state, no side effects.
        PistonStructureResolver resolver = new PistonStructureResolver(level, pos, direction, extending);
        if (!resolver.resolve()) {
            // moveBlocks would return false without moving anything; Paper
            // fires no event in this case either.
            return;
        }
        List<BlockPos> toPush = resolver.getToPush();
        List<BlockPos> toDestroy = resolver.getToDestroy();

        // Extend pushes blocks in the piston's facing; a sticky retract pulls
        // them back toward the piston (opposite of the facing) - the same
        // mapping the Paper bridge applies to the Bukkit event's BlockFace.
        Direction moveDir = extending ? direction : direction.getOpposite();

        List<RBlockPos> sources = new ArrayList<>(toPush.size() + toDestroy.size());
        List<RBlockPos> destinations = new ArrayList<>(toPush.size() + toDestroy.size());
        for (BlockPos block : toPush) {
            sources.add(toRBlockPos(block));
            destinations.add(toRBlockPos(block.relative(moveDir)));
        }
        for (BlockPos block : toDestroy) {
            sources.add(toRBlockPos(block));
            destinations.add(toRBlockPos(block.relative(moveDir)));
        }

        // #if VERSION >= 1.21.11
        String worldId = ((ServerLevel) level).dimension().identifier().toString();
        // #else
        String worldId = ((ServerLevel) level).dimension().location().toString();
        // #endif
        RWorldRef worldRef = new RWorldRef(null, worldId);

        PistonMovePre pre = new PistonMovePre(
            worldRef,
            sources,
            destinations,
            extending ? PistonMovePre.Action.EXTEND : PistonMovePre.Action.RETRACT
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            // "Nothing moved" - the caller (triggerEvent extension path)
            // treats a failed moveBlocks as "piston does not extend".
            cir.setReturnValue(false);
        }
    }

    /**
     * Retraction cases that do not reach {@code moveBlocks}, mirroring Paper's
     * firing surface exactly:
     * <ul>
     *   <li>non-sticky pistons (any retract type): empty-list retract event,</li>
     *   <li>sticky pistons with nothing to pull (head is air): empty-list
     *       retract event,</li>
     *   <li>sticky pistons whose head block is already a moving piston
     *       (mid-animation): no event (Paper skips),</li>
     *   <li>sticky pistons with an unpushable head (e.g. obsidian): no event
     *       (Paper skips - only the head is removed),</li>
     *   <li>sticky pull with pushable blocks: {@code moveBlocks} fires the
     *       event with the real block list (no event here),</li>
     *   <li>sticky pistons receiving retract type 2: no event (Paper skips).</li>
     * </ul>
     */
    @Inject(method = "triggerEvent", at = @At("HEAD"), cancellable = true)
    private void onPistonRetractPre(BlockState state, Level level, BlockPos pos, int type, int data,
                                    CallbackInfoReturnable<Boolean> cir) {
        // type 0 = extension, covered by the moveBlocks injection.
        if (type == 0) return;
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPreListeners(PistonMovePre.class)) return;
        if (!(level instanceof ServerLevel)) return;

        if (isSticky) {
            if (type != 1) return; // sticky type 2: Paper fires no event
            Direction facing = state.getValue(PistonBaseBlock.FACING);
            BlockPos headPos = pos.relative(facing, 2);
            // Moving piston already mid-flight in the same direction: Paper
            // finalTicks it and fires no event.
            if (level.getBlockEntity(headPos) instanceof PistonMovingBlockEntity moving
                && moving.getDirection() == facing && moving.isExtending()) {
                return;
            }
            BlockState headState = level.getBlockState(headPos);
            if (headState.isAir()) {
                // Sticky retract with nothing to pull: Paper fires an
                // empty-list retract event here.
                dispatchEmptyRetract(bus, level, cir);
                return;
            }
            // Pushable head: moveBlocks(false) is called and fires the event
            // with the real block list. Unpushable non-air head (obsidian
            // etc.): Paper fires no event.
            if (PistonBaseBlock.isPushable(headState, level, headPos, facing.getOpposite(), false, facing)
                && (headState.getPistonPushReaction() == PushReaction.NORMAL
                    || headState.is(Blocks.PISTON) || headState.is(Blocks.STICKY_PISTON))) {
                return;
            }
            return;
        }

        // Non-sticky contract (type 1 or 2): Paper fires an empty-list retract
        // event before the head is removed.
        dispatchEmptyRetract(bus, level, cir);
    }

    private void dispatchEmptyRetract(GameEventBus bus, Level level, CallbackInfoReturnable<Boolean> cir) {
        // #if VERSION >= 1.21.11
        String worldId = ((ServerLevel) level).dimension().identifier().toString();
        // #else
        String worldId = ((ServerLevel) level).dimension().location().toString();
        // #endif
        RWorldRef worldRef = new RWorldRef(null, worldId);

        PistonMovePre pre = new PistonMovePre(
            worldRef,
            List.of(),
            List.of(),
            PistonMovePre.Action.RETRACT
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            // No moving block entity is placed, the head is not removed - the
            // piston stays extended.
            cir.setReturnValue(false);
        }
    }

    private static RBlockPos toRBlockPos(BlockPos pos) {
        return new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
    }
}
