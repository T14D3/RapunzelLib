package de.t14d3.rapunzellib.visuals;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for a block structure visual.
 *
 * @param shape        the block structure shape
 * @param color        the structure color
 * @param glow         whether the structure blocks should glow
 * @param viewDistance the maximum view distance in blocks
 */
public record BlockStructureConfig(
    @NotNull BlockStructureShape shape,
    @NotNull TextColor color,
    boolean glow,
    double viewDistance
) implements VisualConfig {
    @Override
    public double viewDistance() {
        return viewDistance;
    }
}
