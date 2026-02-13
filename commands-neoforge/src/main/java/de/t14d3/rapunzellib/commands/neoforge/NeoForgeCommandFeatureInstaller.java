package de.t14d3.rapunzellib.commands.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.AbstractSharedCommandFeatureInstaller;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeCommandFeatureInstaller extends AbstractSharedCommandFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }
}
