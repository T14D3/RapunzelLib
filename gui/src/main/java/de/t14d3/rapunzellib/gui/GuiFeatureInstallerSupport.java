package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Support utilities for GUI feature installation.
 * <p>
 * Provides convenience methods for invoking an installer and for registering
 * a {@link GuiRenderer} in the context.
 * </p>
 */
public final class GuiFeatureInstallerSupport {
    private GuiFeatureInstallerSupport() {
    }

    /**
     * Installs GUI features using the given installer.
     *
     * @param context   the Rapunzel context to install into
     * @param installer the platform-specific installer
     */
    public static void install(
        @NotNull RapunzelContext context,
        @NotNull GuiFeatureInstaller installer
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(installer, "installer");

        installer.install(context);
    }

    /**
     * Registers a {@link GuiRenderer} in the given context.
     *
     * @param context  the Rapunzel context
     * @param renderer the renderer to register
     */
    public static void registerGuiRenderer(
        @NotNull RapunzelContext context,
        @NotNull GuiRenderer renderer
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(renderer, "renderer");

        context.register(GuiRenderer.class, renderer);
    }
}
