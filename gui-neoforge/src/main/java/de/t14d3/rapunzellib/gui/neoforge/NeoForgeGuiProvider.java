package de.t14d3.rapunzellib.gui.neoforge;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.gui.GuiFeatureInstallerSupport;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeGuiProvider {
    private NeoForgeGuiProvider() {
    }

    public static void register(@NotNull RapunzelContext ctx) {
        GuiFeatureInstallerSupport.install(ctx, new NeoForgeGuiFeatureInstaller());
    }
}
