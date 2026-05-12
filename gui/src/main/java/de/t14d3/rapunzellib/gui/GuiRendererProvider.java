package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Provides one or more {@link GuiRenderer} instances for a given context.
 * <p>
 * Used by {@link AbstractGuiFeatureInstaller} to obtain renderers during installation.
 * </p>
 */
public interface GuiRendererProvider {
    /**
     * Provides renderers for the given context.
     *
     * @param context the Rapunzel context
     * @return a collection of renderers
     */
    @NotNull Collection<GuiRenderer> provideRenderers(@NotNull RapunzelContext context);
}
