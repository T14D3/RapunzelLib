package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for installing and accessing the visuals feature.
 * <p>
 * Uses a {@link FeatureInstallerRegistry} to resolve the platform-specific
 * {@link VisualFeatureInstaller}, then delegates to {@link FeatureInstallationSupport}
 * to perform the actual installation.
 */
public final class VisualFeatures {

    private static final FeatureInstallerRegistry<VisualFeatureInstaller> INSTALLER_REGISTRY = FeatureInstallerRegistry.create(
        VisualFeatureInstaller.class,
        VisualFeatureInstaller::platformId,
        "rapunzellib-visuals-"
    );

    private VisualFeatures() {
    }

    /**
     * Installs the visuals feature using the default {@link Rapunzel} context.
     *
     * @return the installed visuals entry point
     */
    public static @NotNull Visuals install() {
        return install(Rapunzel.context());
    }

    /**
     * Installs the visuals feature using the given context.
     *
     * @param ctx the Rapunzel context
     * @return the installed visuals entry point
     */
    public static @NotNull Visuals install(@NotNull RapunzelContext ctx) {
        return FeatureInstallationSupport.install(
            ctx,
            Visuals.class,
            RuntimeCapability.VISUALS,
            "Visual features",
            () -> INSTALLER_REGISTRY.resolve(ctx.platformId()).install(ctx)
        );
    }

    /**
     * Convenience method equivalent to {@link #install()}.
     *
     * @return the installed visuals entry point
     */
    public static @NotNull Visuals visuals() {
        return install();
    }
}
