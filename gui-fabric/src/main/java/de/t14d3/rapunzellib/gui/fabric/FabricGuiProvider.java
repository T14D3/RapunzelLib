package de.t14d3.rapunzellib.gui.fabric;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.gui.GuiFeatureInstallerSupport;
import org.jetbrains.annotations.NotNull;

public final class FabricGuiProvider {

    private FabricGuiProvider() {
    }

    public static void register(@NotNull RapunzelContext ctx) {
        GuiFeatureInstallerSupport.install(ctx, new FabricGuiFeatureInstaller());
    }
}
