package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for installing and accessing inventory features within a Rapunzel context.
 * <p>
 * Uses a {@link FeatureInstallerRegistry} to resolve the correct
 * {@link InventoryFeatureInstaller} for the current platform, then delegates
 * to {@link FeatureInstallationSupport} for lifecycle management.
 */
public final class InventoryFeatures {
    private static final FeatureInstallerRegistry<InventoryFeatureInstaller> INSTALLER_REGISTRY = FeatureInstallerRegistry.create(
        InventoryFeatureInstaller.class,
        InventoryFeatureInstaller::platformId,
        "rapunzellib-inventory-"
    );

    private InventoryFeatures() {
    }

    /**
     * Installs inventory features using the default {@link Rapunzel#context()}.
     *
     * @return the installed {@link Inventories} service
     */
    public static @NotNull Inventories install() {
        return install(Rapunzel.context());
    }

    /**
     * Installs inventory features into the given context by resolving and running
     * the platform-specific {@link InventoryFeatureInstaller}.
     *
     * @param context the Rapunzel context to install into
     * @return the installed {@link Inventories} service
     */
    public static @NotNull Inventories install(@NotNull RapunzelContext context) {
        return FeatureInstallationSupport.install(
            context,
            Inventories.class,
            RuntimeCapability.INVENTORY,
            "inventory features",
            () -> INSTALLER_REGISTRY.resolve(context.platformId()).install(context)
        );
    }

    /**
     * Convenience method equivalent to {@link #install()}.
     *
     * @return the installed {@link Inventories} service
     */
    public static @NotNull Inventories inventories() {
        return install();
    }
}
