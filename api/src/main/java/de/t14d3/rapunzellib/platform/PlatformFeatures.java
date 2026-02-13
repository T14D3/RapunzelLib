package de.t14d3.rapunzellib.platform;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;

public final class PlatformFeatures {
    private static final FeatureInstallerRegistry<PlatformFeatureInstaller> INSTALLER_REGISTRY = FeatureInstallerRegistry.create(
        PlatformFeatureInstaller.class,
        PlatformFeatureInstaller::platformId,
        platformId -> "Add dependency rapunzellib-platform-" + platformId.name().toLowerCase() + '.'
    );

    private PlatformFeatures() {
    }

    public static void install() {
        install(Rapunzel.context());
    }

    public static void install(RapunzelContext context) {
        FeatureInstallationSupport.install(
            context,
            InstallationMarker.class,
            InstallationMarker.INSTANCE,
            null,
            "platform services",
            () -> INSTALLER_REGISTRY.resolve(context.platformId()).install(context)
        );
    }

    private enum InstallationMarker {
        INSTANCE
    }
}
