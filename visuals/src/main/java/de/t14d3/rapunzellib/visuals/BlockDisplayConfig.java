package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.registry.RBlockType;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

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
