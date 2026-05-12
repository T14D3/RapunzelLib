package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.registry.RBlockType;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Configuration for a glow outline visual.
 *
 * @param blocks       the set of block positions to outline
 * @param outlineBlock the block type used for the outline
 * @param color        the outline color
 * @param viewDistance the maximum view distance in blocks
 */
public record GlowOutlineConfig(
    @NotNull Set<RBlockPos> blocks,
    @NotNull RBlockType outlineBlock,
    @NotNull TextColor color,
    double viewDistance
) implements VisualConfig {
    @Override
    public double viewDistance() {
        return viewDistance;
    }
}
