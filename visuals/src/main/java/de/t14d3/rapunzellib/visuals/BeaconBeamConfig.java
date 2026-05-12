package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for a beacon beam visual.
 *
 * @param location      the beacon location
 * @param color         the beam color
 * @param pyramidLevels the number of pyramid levels (0-4)
 * @param extendToSky   whether the beam extends to the sky
 * @param colorRenderer optional custom renderer for glass column colors
 * @param viewDistance  the maximum view distance in blocks
 */
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
