package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

public interface BeaconBeamVisual extends Visual<BeaconBeamConfig> {

    /**
     * Updates the number of pyramid levels.
     *
     * @param levels the pyramid level count (0-4)
     */
    void updatePyramid(int levels);

    /**
     * Updates whether the beam's glass column extends to the sky.
     *
     * @param extendToSky {@code true} to extend the beam to the sky
     */
    void updateGlassColumn(boolean extendToSky);
}
