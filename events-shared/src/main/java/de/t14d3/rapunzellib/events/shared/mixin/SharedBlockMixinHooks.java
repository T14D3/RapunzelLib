package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.block.BlockDestroyPre;
import de.t14d3.rapunzellib.events.block.BlockDestroyUtil;
import de.t14d3.rapunzellib.events.block.BlockEventDispatchUtil;
import de.t14d3.rapunzellib.events.block.BlockFormPre;
import de.t14d3.rapunzellib.events.block.BlockFormUtil;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPost;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPre;
import de.t14d3.rapunzellib.events.block.BlockSpreadPre;
import de.t14d3.rapunzellib.events.block.BlockSpreadUtil;
import de.t14d3.rapunzellib.events.block.BlockTransformPre;
import de.t14d3.rapunzellib.events.block.BlockTransformUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Shared mixin hooks for dispatching block-related events (form, spread, transform, physics).
 * <p>
 * These hooks are called from platform-specific mixins to fire events
 * through the {@link GameEventBus}.
 */
public final class SharedBlockMixinHooks {
    private SharedBlockMixinHooks() {
    }

    /**
     * Thread-local flag set by platform bridges (e.g. Fabric
     * {@code PlayerBlockBreakEvents} callbacks) while a player-initiated
     * block break is in progress. Block destroy mixins consult
     * {@link #isPlayerBreakInProgress()} to skip dispatching
     * {@link BlockDestroyPre} for the {@code Level.setBlock} call that
     * happens as part of the player break flow (the break itself is
     * surfaced via {@code BlockBreakPre} instead).
     */
    private static final ThreadLocal<Boolean> PLAYER_BREAK_IN_PROGRESS =
        ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Marks the start of a player-initiated block break on the current
     * thread. Bridges MUST pair this with {@link #endPlayerBreak()} once
     * the break flow completes (whether cancelled or successful).
     */
    public static void beginPlayerBreak() {
        PLAYER_BREAK_IN_PROGRESS.set(Boolean.TRUE);
    }

    /**
     * Marks the end of a player-initiated block break on the current
     * thread. Safe to call unconditionally - clears the flag set by
     * {@link #beginPlayerBreak()}.
     */
    public static void endPlayerBreak() {
        PLAYER_BREAK_IN_PROGRESS.remove();
    }

    /**
     * @return whether a player-initiated block break is currently in
     * progress on the current thread (so destroy mixins should skip).
     */
    public static boolean isPlayerBreakInProgress() {
        return PLAYER_BREAK_IN_PROGRESS.get();
    }

    /**
     * Dispatches a block form pre-event.
     *
     * @param bus   the game event bus
     * @param level the server level
     * @param pos   the block position
     * @param state the new block state
     * @return {@code true} if the event was cancelled
     */
    public static boolean dispatchBlockFormPre(
        @NotNull GameEventBus bus,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull BlockState state
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        if (!bus.hasPreListeners(BlockFormPre.class)) {
            return false;
        }

        BlockState currentState = level.getBlockState(pos);
        if (currentState == state) {
            return false;
        }

        RKey newBlockKey = blockKey(state);
        RKey sourceBlockKey = blockKey(currentState);
        if (!BlockFormUtil.isFormationEvent(newBlockKey.asString(), sourceBlockKey.asString())) {
            return false;
        }

        return BlockEventDispatchUtil.dispatchBlockFormPre(
            bus,
            worldKey(level),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            newBlockKey,
            sourceBlockKey
        );
    }

    /**
     * Dispatches a block spread pre-event.
     *
     * @param bus   the game event bus
     * @param level the server level
     * @param pos   the block position
     * @param state the new block state
     * @return {@code true} if the event was cancelled
     */
    public static boolean dispatchBlockSpreadPre(
        @NotNull GameEventBus bus,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull BlockState state
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        if (!bus.hasPreListeners(BlockSpreadPre.class)) {
            return false;
        }

        BlockState currentState = level.getBlockState(pos);
        if (currentState == state) {
            return false;
        }

        RKey newBlockKey = blockKey(state);
        RKey sourceBlockKey = blockKey(currentState);
        if (!BlockSpreadUtil.isSpreadEvent(newBlockKey.asString(), sourceBlockKey.asString())) {
            return false;
        }

        return BlockEventDispatchUtil.dispatchBlockSpreadPre(
            bus,
            worldKey(level),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            newBlockKey,
            sourceBlockKey
        );
    }

    /**
     * Dispatches a block transform pre-event.
     *
     * @param bus   the game event bus
     * @param level the server level
     * @param pos   the block position
     * @param state the new block state
     * @return {@code true} if the event was cancelled
     */
    public static boolean dispatchBlockTransformPre(
        @NotNull GameEventBus bus,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull BlockState state
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        if (!bus.hasPreListeners(BlockTransformPre.class)) {
            return false;
        }

        BlockState currentState = level.getBlockState(pos);
        if (currentState == state) {
            return false;
        }

        RKey newBlockKey = blockKey(state);
        RKey originalBlockKey = blockKey(currentState);
        if (!BlockTransformUtil.isTransformEvent(newBlockKey.asString(), originalBlockKey.asString())) {
            return false;
        }

        return BlockEventDispatchUtil.dispatchBlockTransformPre(
            bus,
            worldKey(level),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            originalBlockKey,
            newBlockKey
        );
    }

    /**
     * Dispatches a block destroy pre-event.
     *
     * <p>Returns {@code false} (no-op) when no pre-listeners are registered,
     * when the change is not a destroy event (per {@link BlockDestroyUtil}),
     * or when a player-initiated block break is currently in progress on
     * the calling thread (so the {@code Level.setBlock} call inside the
     * player break flow does not double-fire as a destroy event).</p>
     *
     * @param bus   the game event bus
     * @param level the server level
     * @param pos   the block position
     * @param state the new (replacement) block state
     * @return {@code true} if the event was cancelled
     */
    public static boolean dispatchBlockDestroyPre(
        @NotNull GameEventBus bus,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull BlockState state
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        if (!bus.hasPreListeners(BlockDestroyPre.class)) {
            return false;
        }
        if (isPlayerBreakInProgress()) {
            return false;
        }

        BlockState currentState = level.getBlockState(pos);
        if (currentState == state) {
            return false;
        }

        RKey newBlockKey = blockKey(state);
        RKey sourceBlockKey = blockKey(currentState);
        if (!BlockDestroyUtil.isDestroyEvent(newBlockKey.asString(), sourceBlockKey.asString())) {
            return false;
        }

        return BlockEventDispatchUtil.dispatchBlockDestroyPre(
            bus,
            worldKey(level),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            newBlockKey
        );
    }

    /**
     * Dispatches a block physics neighbor pre-event.
     *
     * @param bus          the game event bus
     * @param level        the server level
     * @param pos          the block position
     * @param changedBlock the block that changed
     * @return {@code true} if the event was cancelled
     */
    public static boolean dispatchBlockPhysicsNeighborPre(
        @NotNull GameEventBus bus,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull Block changedBlock
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(changedBlock, "changedBlock");
        boolean needsPre = bus.hasPreListeners(BlockPhysicsPre.class);
        boolean needsPost = bus.hasPostListeners(BlockPhysicsPost.class);
        if (!needsPre && !needsPost) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return BlockEventDispatchUtil.dispatchBlockPhysicsPre(
            bus,
            needsPre,
            needsPost,
            worldKey(level),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            blockKey(state),
            blockKey(changedBlock)
        );
    }

    /**
     * Dispatches a block physics neighbor post-event.
     *
     * @param bus          the game event bus
     * @param level        the server level
     * @param pos          the block position
     * @param changedBlock the block that changed
     */
    public static void dispatchBlockPhysicsNeighborPost(
        @NotNull GameEventBus bus,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull Block changedBlock
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(changedBlock, "changedBlock");
        if (!bus.hasPostListeners(BlockPhysicsPost.class)) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        BlockEventDispatchUtil.dispatchBlockPhysicsPost(
            bus,
            worldKey(level),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            blockKey(state),
            blockKey(changedBlock),
            false
        );
    }

    /**
     * Dispatches a block physics state change pre-event (same block type, different state).
     *
     * @param bus      the game event bus
     * @param level    the server level
     * @param pos      the block position
     * @param oldState the old block state
     * @param newState the new block state
     * @return {@code true} if the event was cancelled
     */
    public static boolean dispatchBlockPhysicsStateChangePre(
        @NotNull GameEventBus bus,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull BlockState oldState,
        @NotNull BlockState newState
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(oldState, "oldState");
        Objects.requireNonNull(newState, "newState");
        if (oldState.getBlock() != newState.getBlock()) {
            return false;
        }

        boolean needsPre = bus.hasPreListeners(BlockPhysicsPre.class);
        boolean needsPost = bus.hasPostListeners(BlockPhysicsPost.class);
        if (!needsPre && !needsPost) {
            return false;
        }

        return BlockEventDispatchUtil.dispatchBlockPhysicsPre(
            bus,
            needsPre,
            needsPost,
            worldKey(level),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            blockKey(newState),
            blockKey(newState.getBlock())
        );
    }

    private static @NotNull RKey worldKey(@NotNull ServerLevel level) {
        // #if VERSION >= 1.21.11
        return RKey.of(level.dimension().identifier().toString());
        // #else
        return RKey.of(level.dimension().location().toString());
        // #endif
    }

    private static @NotNull RKey blockKey(@NotNull BlockState state) {
        return RKey.of(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    private static @NotNull RKey blockKey(@NotNull Block block) {
        return RKey.of(BuiltInRegistries.BLOCK.getKey(block).toString());
    }
}
