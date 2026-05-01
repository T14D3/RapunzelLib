package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSharedCommandFeatureInstaller implements CommandFeatureInstaller {
    protected @NotNull SharedCommandFeatureInstallerSupport.CommandSourceStackAdapterSpec<?> commandSourceStackAdapterSpec() {
        return SharedCommandFeatureInstallerSupport.defaultCommandSourceStackAdapterSpec(platformId());
    }

    @Override
    public final void install(@NotNull RapunzelContext context) {
        SharedCommandFeatureInstallerSupport.installCommandSourceStackSupport(context, commandSourceStackAdapterSpec());
    }
}
