package de.t14d3.rapunzellib.events.block;

/**
 * Utility for detecting block spread events (fire, mushrooms, vines, bamboo, etc.).
 */
public final class BlockSpreadUtil {
    private BlockSpreadUtil() {
    }

    /**
     * Checks whether a block change is a spread event.
     *
     * @param newBlockKey    the key of the spreading block
     * @param sourceBlockKey the key of the source block
     * @return true if this is a spread event
     */
    public static boolean isSpreadEvent(String newBlockKey, String sourceBlockKey) {
        if (newBlockKey.equals("minecraft:fire")) return true;
        if (newBlockKey.equals("minecraft:soul_fire")) return true;
        if (newBlockKey.equals("minecraft:brown_mushroom") || newBlockKey.equals("minecraft:red_mushroom")) return true;
        if (newBlockKey.equals("minecraft:vine")) return true;
        if (newBlockKey.equals("minecraft:cave_vines") || newBlockKey.equals("minecraft:cave_vines_plant")) return true;
        if (newBlockKey.equals("minecraft:weeping_vines") || newBlockKey.equals("minecraft:weeping_vines_plant")) return true;
        if (newBlockKey.equals("minecraft:twisting_vines") || newBlockKey.equals("minecraft:twisting_vines_plant")) return true;
        if (newBlockKey.equals("minecraft:bamboo") || newBlockKey.equals("minecraft:bamboo_sapling")) return true;
        if (newBlockKey.equals("minecraft:chorus_plant") || newBlockKey.equals("minecraft:chorus_flower")) return true;
        if (newBlockKey.equals("minecraft:kelp") || newBlockKey.equals("minecraft:kelp_plant")) return true;
        if (newBlockKey.equals("minecraft:sugar_cane")) return true;
        return newBlockKey.equals("minecraft:cactus");
    }
}
