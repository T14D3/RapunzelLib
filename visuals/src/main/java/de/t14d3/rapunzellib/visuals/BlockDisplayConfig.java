package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.registry.RBlockType;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for a block display visual.
 *
 * @param block      the block type to display
 * @param transform  the display transformation (translation, scale, rotation)
 * @param color      the overlay color
 * @param glow       whether the display entity should glow
 * @param viewDistance the maximum view distance in blocks
 */
public record BlockDisplayConfig(
    @NotNull RBlockType block,
    @NotNull DisplayTransform transform,
    @NotNull TextColor color,
    boolean glow,
    double viewDistance
) implements VisualConfig {
    @Override
    public double viewDistance() {
        return viewDistance;
    }
}
