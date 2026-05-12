package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base for platform-specific command feature installers.
 * <p>
 * Delegates command source stack adapter registration to
 * {@link SharedCommandFeatureInstallerSupport} using the default spec.
 */
public abstract class AbstractSharedCommandFeatureInstaller implements CommandFeatureInstaller {
    /**
     * Provides the default {@link SharedCommandFeatureInstallerSupport.CommandSourceStackAdapterSpec}
     * for the associated platform.
     *
     * @return the default adapter spec
     */
    protected @NotNull SharedCommandFeatureInstallerSupport.CommandSourceStackAdapterSpec<?> commandSourceStackAdapterSpec() {
        return SharedCommandFeatureInstallerSupport.defaultCommandSourceStackAdapterSpec(platformId());
    }

    @Override
    public final void install(@NotNull RapunzelContext context) {
        SharedCommandFeatureInstallerSupport.installCommandSourceStackSupport(context, commandSourceStackAdapterSpec());
    }
}
