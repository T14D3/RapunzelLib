package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GuiFeatureInstallerSupport {
    private GuiFeatureInstallerSupport() {
    }

    public static void install(
        @NotNull RapunzelContext context,
        @NotNull GuiFeatureInstaller installer
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(installer, "installer");

        installer.install(context);
    }

    public static void registerGuiRenderer(
        @NotNull RapunzelContext context,
        @NotNull GuiRenderer renderer
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(renderer, "renderer");

        context.register(GuiRenderer.class, renderer);
    }
}
