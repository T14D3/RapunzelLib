package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

public final class VisualFeatures {
    private static final FeatureInstallerRegistry<VisualFeatureInstaller> INSTALLER_REGISTRY = FeatureInstallerRegistry.create(
        VisualFeatureInstaller.class,
        VisualFeatureInstaller::platformId,
        "rapunzellib-visuals-"
    );

    private VisualFeatures() {
    }

    public static @NotNull Visuals install() {
        return install(Rapunzel.context());
    }

    public static @NotNull Visuals install(@NotNull RapunzelContext ctx) {
        return FeatureInstallationSupport.install(
            ctx,
            Visuals.class,
            RuntimeCapability.VISUALS,
            "Visual features",
            () -> INSTALLER_REGISTRY.resolve(ctx.platformId()).install(ctx)
        );
    }

    public static @NotNull Visuals visuals() {
        return install();
    }
}
