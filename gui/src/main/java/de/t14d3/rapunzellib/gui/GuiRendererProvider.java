package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface GuiRendererProvider {
    @NotNull Collection<GuiRenderer> provideRenderers(@NotNull RapunzelContext context);
}
