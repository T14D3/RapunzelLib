package de.t14d3.rapunzellib.context.fixtures;

import de.t14d3.rapunzellib.PlatformId;

public final class TestCompleteVelocityFeatureInstaller implements TestCompleteFeatureInstaller {
    @Override
    public PlatformId platformId() {
        return PlatformId.VELOCITY;
    }
}
