package de.t14d3.rapunzellib.visuals;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Maps Minecraft dye colors to Adventure {@link NamedTextColor} constants.
 * <p>
 * Provides bidirectional lookup: from dye name to Adventure color,
 * and from Adventure color back to the dye enum value.
 */
public enum DyeColor {
    WHITE(NamedTextColor.WHITE),
    ORANGE(NamedTextColor.GOLD),
    MAGENTA(NamedTextColor.LIGHT_PURPLE),
    LIGHT_BLUE(NamedTextColor.AQUA),
    YELLOW(NamedTextColor.YELLOW),
    LIME(NamedTextColor.GREEN),
    PINK(NamedTextColor.RED),
    GRAY(NamedTextColor.GRAY),
    LIGHT_GRAY(NamedTextColor.DARK_GRAY),
    CYAN(NamedTextColor.DARK_AQUA),
    PURPLE(NamedTextColor.DARK_PURPLE),
    BLUE(NamedTextColor.BLUE),
    BROWN(NamedTextColor.DARK_GREEN),
    GREEN(NamedTextColor.DARK_GREEN),
    RED(NamedTextColor.DARK_RED),
    BLACK(NamedTextColor.BLACK);

    private final NamedTextColor namedColor;

    DyeColor(NamedTextColor namedColor) {
        this.namedColor = namedColor;
    }

    public NamedTextColor adventureColor() {
        return namedColor;
    }

    /**
     * Converts an Adventure named text color back to the closest dye color.
     *
     * @param color the named text color to convert
     * @return the matching dye color, or {@link #WHITE} if no match is found
     */
    public static DyeColor fromAdventure(NamedTextColor color) {
        if (color == null) return WHITE;
        for (DyeColor dye : values()) {
            if (dye.namedColor.equals(color)) return dye;
        }
        return WHITE;
    }
}
