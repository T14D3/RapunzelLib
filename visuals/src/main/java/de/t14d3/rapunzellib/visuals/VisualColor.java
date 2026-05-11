package de.t14d3.rapunzellib.visuals;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public record VisualColor(@NotNull TextColor textColor) {
    public static @NotNull VisualColor dye(@NotNull DyeColor dye) {
        return new VisualColor(dye.adventureColor());
    }

    public static @NotNull VisualColor hex(int rgb) {
        return new VisualColor(TextColor.color(rgb));
    }

    public int rgbValue() {
        if (textColor instanceof net.kyori.adventure.text.format.NamedTextColor named) {
            return switch (named.toString()) {
                case "white" -> 0xFFFFFF;
                case "gold" -> 0xFFAA00;
                case "light_purple" -> 0xFF55FF;
                case "aqua" -> 0x55FFFF;
                case "yellow" -> 0xFFFF55;
                case "green" -> 0x55FF55;
                case "red" -> 0xFF5555;
                case "gray" -> 0xAAAAAA;
                case "dark_gray" -> 0x555555;
                case "dark_aqua" -> 0x00AAAA;
                case "dark_purple" -> 0xAA00AA;
                case "blue" -> 0x5555FF;
                case "dark_green" -> 0x00AA00;
                case "dark_red" -> 0xAA0000;
                case "black" -> 0x000000;
                default -> 0xFFFFFF;
            };
        }
        return textColor.value();
    }
}
