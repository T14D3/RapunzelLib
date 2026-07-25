package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;

/**
 * Utility class for dispatching block-related events via the {@link GameEventBus}.
 *
 * <p>Provides static helper methods for creating and dispatching block form, spread,
 * transform, and physics events from platform bridge code.</p>
 *
 * <p>Bridge callers typically only know the world key and the integer coordinates of
 * the affected block together with the involved type keys. These helpers synthesize
 * the live {@link RBlock} wrappers (via {@link RBlock#at(RWorld, RBlockPos)}) and
 * resolve {@link RBlockType}s (via {@link RBlockType#require(RKey)}) before
 * constructing the events.</p>
 */
public final class BlockEventDispatchUtil {
    private BlockEventDispatchUtil() {
    }

    /**
     * Dispatches a {@link BlockFormPre} event and returns whether it was denied.
     *
     * @param bus           the event bus
     * @param worldKey      the world key
     * @param x             the x coordinate
     * @param y             the y coordinate
     * @param z             the z coordinate
     * @param newBlockKey   the key of the forming block
     * @param sourceBlockKey the key of the source block
     * @return true if the event was denied
     */
    public static boolean dispatchBlockFormPre(
            GameEventBus bus,
            RKey worldKey,
            int x,
            int y,
            int z,
            RKey newBlockKey,
            RKey sourceBlockKey
    ) {
        RBlockPos pos = blockPos(x, y, z);
        RBlock block = blockAt(worldKey, pos);
        BlockFormPre pre = new BlockFormPre(block, RBlockType.require(newBlockKey));
        bus.dispatchPre(pre);
        return pre.isDenied();
    }

    /**
     * Dispatches a {@link BlockSpreadPre} event and returns whether it was denied.
     *
     * <p>Note: the legacy signature only carries a single world+position plus the two
     * type keys. The {@link BlockSpreadPre} event now requires two live {@link RBlock}
     * wrappers, so this helper synthesizes both from the supplied world+position
     * (the actual donor/source location must be supplied by future bridge revisions
     * with richer coordinates).</p>
     *
     * @param bus           the event bus
     * @param worldKey      the world key
     * @param x             the x coordinate
     * @param y             the y coordinate
     * @param z             the z coordinate
     * @param newBlockKey   the key of the spreading block
     * @param sourceBlockKey the key of the source block
     * @return true if the event was denied
     */
    public static boolean dispatchBlockSpreadPre(
            GameEventBus bus,
            RKey worldKey,
            int x,
            int y,
            int z,
            RKey newBlockKey,
            RKey sourceBlockKey
    ) {
        RBlockPos pos = blockPos(x, y, z);
        RBlock block = blockAt(worldKey, pos);
        // The legacy dispatch did not carry separate donor coordinates; synthesize a
        // source block at the same position. Bridges with richer coordinates should
        // construct BlockSpreadPre directly.
        RBlock source = block;
        BlockSpreadPre pre = new BlockSpreadPre(block, source);
        bus.dispatchPre(pre);
        return pre.isDenied();
    }

    /**
     * Dispatches a {@link BlockTransformPre} event and returns whether it was denied.
     *
     * @param bus             the event bus
     * @param worldKey        the world key
     * @param x               the x coordinate
     * @param y               the y coordinate
     * @param z               the z coordinate
     * @param originalBlockKey the key of the original block
     * @param newBlockKey     the key of the transformed block
     * @return true if the event was denied
     */
    public static boolean dispatchBlockTransformPre(
            GameEventBus bus,
            RKey worldKey,
            int x,
            int y,
            int z,
            RKey originalBlockKey,
            RKey newBlockKey
    ) {
        RBlockPos pos = blockPos(x, y, z);
        RBlock block = blockAt(worldKey, pos);
        BlockTransformPre pre = new BlockTransformPre(block, RBlockType.require(newBlockKey));
        bus.dispatchPre(pre);
        return pre.isDenied();
    }

    /**
     * Dispatches a {@link BlockPhysicsPre} event (and optionally a {@link BlockPhysicsPost}
     * if cancelled) and returns whether the event was denied.
     *
     * @param bus            the event bus
     * @param needsPre       whether to dispatch the pre-event
     * @param needsPost      whether to dispatch the post-event if cancelled
     * @param worldKey       the world key
     * @param x              the x coordinate
     * @param y              the y coordinate
     * @param z              the z coordinate
     * @param blockTypeKey   the block type key (the block undergoing physics)
     * @param changedTypeKey the block type that changed, triggering this physics update
     * @return true if the event was denied
     */
    public static boolean dispatchBlockPhysicsPre(
            GameEventBus bus,
            boolean needsPre,
            boolean needsPost,
            RKey worldKey,
            int x,
            int y,
            int z,
            RKey blockTypeKey,
            RKey changedTypeKey
    ) {
        boolean cancelled = false;

        if (needsPre) {
            RBlockPos pos = blockPos(x, y, z);
            RBlock block = blockAt(worldKey, pos);
            BlockPhysicsPre pre = new BlockPhysicsPre(block, RBlockType.require(changedTypeKey));
            bus.dispatchPre(pre);
            cancelled = pre.isDenied();
        }

        if (cancelled && needsPost) {
            RBlockPos pos = blockPos(x, y, z);
            RBlock block = blockAt(worldKey, pos);
            bus.dispatchPost(new BlockPhysicsPost(block, RBlockType.require(changedTypeKey), true));
        }

        return cancelled;
    }

    /**
     * Dispatches a {@link BlockPhysicsPost} event.
     *
     * @param bus            the event bus
     * @param worldKey       the world key
     * @param x              the x coordinate
     * @param y              the y coordinate
     * @param z              the z coordinate
     * @param blockTypeKey   the block type key
     * @param changedTypeKey the block type that changed, triggering this physics update
     * @param cancelled      whether the physics update was cancelled
     */
    public static void dispatchBlockPhysicsPost(
            GameEventBus bus,
            RKey worldKey,
            int x,
            int y,
            int z,
            RKey blockTypeKey,
            RKey changedTypeKey,
            boolean cancelled
    ) {
        RBlockPos pos = blockPos(x, y, z);
        RBlock block = blockAt(worldKey, pos);
        bus.dispatchPost(new BlockPhysicsPost(block, RBlockType.require(changedTypeKey), cancelled));
    }

    private static RBlockPos blockPos(int x, int y, int z) {
        return new RBlockPos(x, y, z);
    }

    private static RBlock blockAt(RKey worldKey, RBlockPos pos) {
        return RBlock.at(RWorld.require(worldKey), pos);
    }
}
