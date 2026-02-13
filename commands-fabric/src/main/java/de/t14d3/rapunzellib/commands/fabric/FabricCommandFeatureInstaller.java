package de.t14d3.rapunzellib.commands.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.AbstractSharedCommandFeatureInstaller;
import org.jetbrains.annotations.NotNull;

public final class FabricCommandFeatureInstaller extends AbstractSharedCommandFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.FABRIC;
    }
}
