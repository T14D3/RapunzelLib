package de.t14d3.rapunzellib.visuals;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * A wrapper around Adventure's {@link TextColor} that provides
 * convenience factory methods and an RGB extraction helper.
 *
 * @param textColor the underlying Adventure text color
 */
public record VisualColor(@NotNull TextColor textColor) {

    /**
     * Creates a visual color from a dye color.
     *
     * @param dye the dye color
     * @return the visual color
     */
    public static @NotNull VisualColor dye(@NotNull DyeColor dye) {
        return new VisualColor(dye.adventureColor());
    }

    /**
     * Creates a visual color from an RGB integer.
     *
     * @param rgb the RGB value (e.g. {@code 0xFF00FF})
     * @return the visual color
     */
    public static @NotNull VisualColor hex(int rgb) {
        return new VisualColor(TextColor.color(rgb));
    }

    /**
     * Returns the RGB integer value of this color.
     * <p>
     * Named text colors are mapped to their closest Minecraft
     * concrete color equivalents.
     *
     * @return the RGB value as an int
     */
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
