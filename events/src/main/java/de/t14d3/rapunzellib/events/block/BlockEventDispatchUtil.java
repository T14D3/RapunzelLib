package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

public final class BlockEventDispatchUtil {
    private BlockEventDispatchUtil() {
    }

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

    public static boolean dispatchBlockPhysicsPre(
            GameEventBus bus,
            boolean needsPre,
            boolean needsPost,
            RKey worldKey,
            int x,
            int y,
            int z,
            RKey blockTypeKey,
            int changedTypeId
    ) {
        boolean cancelled = false;

        if (needsPre) {
            BlockPhysicsPre pre = new BlockPhysicsPre(worldRef(worldKey), blockPos(x, y, z), blockTypeKey, changedTypeId);
            bus.dispatchPre(pre);
            cancelled = pre.isDenied();
        }

        if (cancelled && needsPost) {
            bus.dispatchPost(new BlockPhysicsPost(worldRef(worldKey), blockPos(x, y, z), blockTypeKey, changedTypeId, true));
        }

        return cancelled;
    }

    public static void dispatchBlockPhysicsPost(
            GameEventBus bus,
            RKey worldKey,
            int x,
            int y,
            int z,
            RKey blockTypeKey,
            int changedTypeId,
            boolean cancelled
    ) {
        bus.dispatchPost(new BlockPhysicsPost(worldRef(worldKey), blockPos(x, y, z), blockTypeKey, changedTypeId, cancelled));
    }

    private static RWorldRef worldRef(RKey worldKey) {
        return new RWorldRef(worldKey.asString(), worldKey);
    }

    private static RBlockPos blockPos(int x, int y, int z) {
        return new RBlockPos(x, y, z);
    }
}
