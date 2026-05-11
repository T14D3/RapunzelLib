package de.t14d3.rapunzellib.visuals;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

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
