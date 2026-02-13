package de.t14d3.rapunzellib.gui.sponge;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.gui.GuiFeatureInstallerSupport;
import org.jetbrains.annotations.NotNull;

public final class SpongeGuiProvider {

    private SpongeGuiProvider() {
    }

    public static void register(@NotNull RapunzelContext ctx) {
        GuiFeatureInstallerSupport.install(ctx, new SpongeGuiFeatureInstaller());
    }
}
