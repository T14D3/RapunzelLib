package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record BeaconBeamConfig(
    @NotNull RLocation location,
    @NotNull TextColor color,
    int pyramidLevels,
    boolean extendToSky,
    @Nullable BeaconColorRenderer colorRenderer,
    double viewDistance
) implements VisualConfig {
    @Override
    public double viewDistance() {
        return viewDistance;
    }
}
