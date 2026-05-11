package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

public interface BeaconBeamVisual extends Visual<BeaconBeamConfig> {
    void updatePyramid(int levels);

    void updateGlassColumn(boolean extendToSky);
}
