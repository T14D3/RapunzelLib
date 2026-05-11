package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.registry.RBlockType;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ConcreteColorResolver {
    private static final Map<NamedTextColor, String> CONCRETE_MAP = new LinkedHashMap<>();

    static {
        CONCRETE_MAP.put(NamedTextColor.WHITE, "minecraft:white_concrete");
        CONCRETE_MAP.put(NamedTextColor.GOLD, "minecraft:orange_concrete");
        CONCRETE_MAP.put(NamedTextColor.LIGHT_PURPLE, "minecraft:magenta_concrete");
        CONCRETE_MAP.put(NamedTextColor.AQUA, "minecraft:light_blue_concrete");
        CONCRETE_MAP.put(NamedTextColor.YELLOW, "minecraft:yellow_concrete");
        CONCRETE_MAP.put(NamedTextColor.GREEN, "minecraft:lime_concrete");
        CONCRETE_MAP.put(NamedTextColor.RED, "minecraft:red_concrete");
        CONCRETE_MAP.put(NamedTextColor.GRAY, "minecraft:light_gray_concrete");
        CONCRETE_MAP.put(NamedTextColor.DARK_GRAY, "minecraft:gray_concrete");
        CONCRETE_MAP.put(NamedTextColor.DARK_AQUA, "minecraft:cyan_concrete");
        CONCRETE_MAP.put(NamedTextColor.DARK_PURPLE, "minecraft:purple_concrete");
        CONCRETE_MAP.put(NamedTextColor.BLUE, "minecraft:blue_concrete");
        CONCRETE_MAP.put(NamedTextColor.DARK_GREEN, "minecraft:green_concrete");
        CONCRETE_MAP.put(NamedTextColor.DARK_RED, "minecraft:red_concrete");
        CONCRETE_MAP.put(NamedTextColor.DARK_BLUE, "minecraft:blue_concrete");
        CONCRETE_MAP.put(NamedTextColor.BLACK, "minecraft:black_concrete");
    }

    private ConcreteColorResolver() {
    }

    public static @NotNull RBlockType resolve(@NotNull TextColor color) {
        NamedTextColor named = color instanceof NamedTextColor n ? n : nearestNamed(color);
        String blockKey = CONCRETE_MAP.getOrDefault(named, "minecraft:white_concrete");
        return RBlockType.require(blockKey);
    }

    public static @NotNull String resolveKey(@NotNull TextColor color) {
        NamedTextColor named = color instanceof NamedTextColor n ? n : nearestNamed(color);
        return CONCRETE_MAP.getOrDefault(named, "minecraft:white_concrete");
    }

    @NotNull
    static NamedTextColor nearestNamed(@NotNull TextColor color) {
        int rgb = color.value();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        NamedTextColor best = NamedTextColor.WHITE;
        double bestDist = Double.MAX_VALUE;

        for (NamedTextColor named : NamedTextColor.NAMES.values()) {
            int nr = (named.value() >> 16) & 0xFF;
            int ng = (named.value() >> 8) & 0xFF;
            int nb = named.value() & 0xFF;

            double dr = r - nr;
            double dg = g - ng;
            double db = b - nb;
            double dist = dr * dr + dg * dg + db * db;

            if (dist < bestDist) {
                bestDist = dist;
                best = named;
            }
        }

        return best;
    }
}
