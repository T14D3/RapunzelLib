package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for installing and accessing GUI features in RapunzelLib.
 * <p>
 * Provides static methods to install the GUI feature set, resolve a renderer,
 * and manage the {@link GuiFeatureInstaller} registry.
 * </p>
 */
public final class GuiFeatures {
    private static final FeatureInstallerRegistry<GuiFeatureInstaller> INSTALLER_REGISTRY = FeatureInstallerRegistry.create(
        GuiFeatureInstaller.class,
        GuiFeatureInstaller::platformId,
        "rapunzellib-gui-"
    );

    private GuiFeatures() {
    }

    public static @NotNull GuiRenderer install() {
        return install(Rapunzel.context());
    }

    public static @NotNull GuiRenderer install(@NotNull RapunzelContext ctx) {
        return FeatureInstallationSupport.install(
            ctx,
            GuiRenderer.class,
            RuntimeCapability.GUI,
            "GUI features",
            () -> INSTALLER_REGISTRY.resolve(ctx.platformId()).install(ctx),
            InventoryFeatures::install
        );
    }

    public static @NotNull GuiRenderer renderer() {
        return install();
    }
}
