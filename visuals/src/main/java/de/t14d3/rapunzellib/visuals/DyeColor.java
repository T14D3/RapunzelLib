package de.t14d3.rapunzellib.visuals;

import net.kyori.adventure.text.format.NamedTextColor;

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

    public static DyeColor fromAdventure(NamedTextColor color) {
        if (color == null) return WHITE;
        for (DyeColor dye : values()) {
            if (dye.namedColor.equals(color)) return dye;
        }
        return WHITE;
    }
}
