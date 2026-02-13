package de.t14d3.rapunzellib.events.block;

import java.util.Set;

/**
 * Utilities for detecting block transformation events (oxidation, waxing, scraping).
 */
public final class BlockTransformUtil {
    private static final String WAXED_PREFIX = "waxed_";
    private static final String COPPER_BLOCK = "minecraft:copper_block";
    private static final String COPPER_BASE = "minecraft:copper";
    private static final Set<String> OXIDIZABLE_BASES = Set.of(
            COPPER_BASE,
            "minecraft:cut_copper",
            "minecraft:cut_copper_slab",
            "minecraft:cut_copper_stairs"
    );
    private static final String[] OXIDATION_PREFIXES = {
            "oxidized_",
            "weathered_",
            "exposed_"
    };

    private BlockTransformUtil() {
    }

    public static boolean isTransformEvent(String newBlockKey, String originalBlockKey) {
        if (isOxidationStep(newBlockKey, originalBlockKey)) return true;
        if (isWaxed(newBlockKey) && !isWaxed(originalBlockKey)) return true;
        if (!isWaxed(newBlockKey) && isWaxed(originalBlockKey)) return true;
        return isLessOxidized(newBlockKey, originalBlockKey);
    }

    private static boolean isOxidationStep(String newBlockKey, String originalBlockKey) {
        int newLevel = getOxidationLevel(newBlockKey);
        int originalLevel = getOxidationLevel(originalBlockKey);
        if (newLevel != originalLevel + 1) return false;

        String newBase = oxidationBase(newBlockKey);
        if (!OXIDIZABLE_BASES.contains(newBase)) return false;

        String originalBase = oxidationBase(originalBlockKey);
        return newBase.equals(originalBase);
    }

    private static boolean isWaxed(String blockKey) {
        return blockKey.contains(WAXED_PREFIX);
    }

    private static String oxidationBase(String blockKey) {
        String base = stripOxidationPrefix(blockKey);
        return COPPER_BLOCK.equals(base) ? COPPER_BASE : base;
    }

    private static String stripOxidationPrefix(String blockKey) {
        String path = blockKey;
        String namespace = "";
        int colon = blockKey.indexOf(':');
        if (colon >= 0) {
            namespace = blockKey.substring(0, colon + 1);
            path = blockKey.substring(colon + 1);
        }

        for (String prefix : OXIDATION_PREFIXES) {
            if (path.startsWith(prefix)) {
                path = path.substring(prefix.length());
                break;
            }
        }

        return namespace + path;
    }

    private static boolean isLessOxidized(String newBlockKey, String originalBlockKey) {
        int newLevel = getOxidationLevel(newBlockKey);
        int originalLevel = getOxidationLevel(originalBlockKey);
        return newLevel < originalLevel && originalLevel > 0;
    }

    private static int getOxidationLevel(String blockKey) {
        if (blockKey.contains("oxidized_")) return 3;
        if (blockKey.contains("weathered_")) return 2;
        if (blockKey.contains("exposed_")) return 1;
        return 0;
    }
}
