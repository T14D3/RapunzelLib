package de.t14d3.rapunzellib.events.block;

public final class BlockFormUtil {
    private BlockFormUtil() {
    }

    public static boolean isFormationEvent(String newBlockKey, String sourceBlockKey) {
        if (newBlockKey.equals("minecraft:ice") && sourceBlockKey.equals("minecraft:water")) return true;
        if (newBlockKey.equals("minecraft:frosted_ice") && sourceBlockKey.equals("minecraft:water")) return true;

        if (newBlockKey.equals("minecraft:snow") || newBlockKey.equals("minecraft:snow_block")) return true;

        if (newBlockKey.contains("concrete") && sourceBlockKey.contains("concrete_powder")) return true;

        if (newBlockKey.equals("minecraft:obsidian")) return true;

        return newBlockKey.equals("minecraft:cobblestone") || newBlockKey.equals("minecraft:stone");
    }
}
