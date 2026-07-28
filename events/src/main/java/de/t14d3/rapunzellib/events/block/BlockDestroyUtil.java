package de.t14d3.rapunzellib.events.block;

/**
 * Utility for detecting block destroy events.
 *
 * <p>A destroy event occurs when a "real" block (not air, not a fluid-only
 * state) is replaced by air or a fluid-only state, e.g. via {@code /setblock
 * ... destroy}, lava consuming blocks, explosions, piston detachment, etc.
 *
 * <p>Player-initiated breaking is handled separately as {@link BlockBreakPre}
 * and excluded from destroy dispatch by the platform mixins via a thread-local
 * guard. Formation, spread, and transformation events are excluded via the
 * companion {@link BlockFormUtil}/{@link BlockSpreadUtil}/
 * {@link BlockTransformUtil} predicates so destroy does not double-fire.</p>
 */
public final class BlockDestroyUtil {
    private BlockDestroyUtil() {
    }

    /**
     * Checks whether a block change is a destroy event.
     *
     * @param newBlockKey    the key of the resulting (replacement) block
     * @param sourceBlockKey the key of the source block being destroyed
     * @return true if this is a destroy event
     */
    public static boolean isDestroyEvent(String newBlockKey, String sourceBlockKey) {
        // The replacement must be air or a fluid-only state; the source must
        // not already be air or a fluid-only block.
        if (!isReplacementEmpty(newBlockKey)) return false;
        if (isReplacementEmpty(sourceBlockKey)) return false;
        // Skip explicit formation/spread/transform events (handled by their
        // own mixins) so destroy does not double-fire.
        if (BlockFormUtil.isFormationEvent(newBlockKey, sourceBlockKey)) return false;
        if (BlockSpreadUtil.isSpreadEvent(newBlockKey, sourceBlockKey)) return false;
        if (BlockTransformUtil.isTransformEvent(newBlockKey, sourceBlockKey)) return false;
        return true;
    }

    private static boolean isReplacementEmpty(String blockKey) {
        return "minecraft:air".equals(blockKey)
            || "minecraft:water".equals(blockKey)
            || "minecraft:lava".equals(blockKey);
    }
}
