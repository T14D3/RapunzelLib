package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

/**
 * Utility class for dispatching block-related events via the {@link GameEventBus}.
 *
 * <p>Provides static helper methods for creating and dispatching block form, spread,
 * transform, and physics events from platform bridge code.</p>
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
        BlockFormPre pre = new BlockFormPre(worldRef(worldKey), blockPos(x, y, z), newBlockKey, sourceBlockKey);
        bus.dispatchPre(pre);
        return pre.isDenied();
    }

    /**
     * Dispatches a {@link BlockSpreadPre} event and returns whether it was denied.
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
        BlockSpreadPre pre = new BlockSpreadPre(worldRef(worldKey), blockPos(x, y, z), newBlockKey, sourceBlockKey);
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
        BlockTransformPre pre = new BlockTransformPre(worldRef(worldKey), blockPos(x, y, z), originalBlockKey, newBlockKey);
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
            BlockPhysicsPre pre = new BlockPhysicsPre(worldRef(worldKey), blockPos(x, y, z), blockTypeKey, changedTypeKey);
            bus.dispatchPre(pre);
            cancelled = pre.isDenied();
        }

        if (cancelled && needsPost) {
            bus.dispatchPost(new BlockPhysicsPost(worldRef(worldKey), blockPos(x, y, z), blockTypeKey, changedTypeKey, true));
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
        bus.dispatchPost(new BlockPhysicsPost(worldRef(worldKey), blockPos(x, y, z), blockTypeKey, changedTypeKey, cancelled));
    }

    private static RWorldRef worldRef(RKey worldKey) {
        return new RWorldRef(worldKey.asString(), worldKey);
    }

    private static RBlockPos blockPos(int x, int y, int z) {
        return new RBlockPos(x, y, z);
    }
}
