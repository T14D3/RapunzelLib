package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

public final class InventoryFeatures {
    private static final FeatureInstallerRegistry<InventoryFeatureInstaller> INSTALLER_REGISTRY = FeatureInstallerRegistry.create(
        InventoryFeatureInstaller.class,
        InventoryFeatureInstaller::platformId,
        "rapunzellib-inventory-"
    );

    private InventoryFeatures() {
    }

    public static @NotNull Inventories install() {
        return install(Rapunzel.context());
    }

    public static @NotNull Inventories install(@NotNull RapunzelContext context) {
        return FeatureInstallationSupport.install(
            context,
            Inventories.class,
            RuntimeCapability.INVENTORY,
            "inventory features",
            () -> INSTALLER_REGISTRY.resolve(context.platformId()).install(context)
        );
    }

    public static @NotNull Inventories inventories() {
        return install();
    }
}
