package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface GuiRenderer {
    @NotNull String name();
    
    @NotNull Set<GuiCapability> capabilities();
    
    boolean supports(@NotNull GuiCapability capability);
    
    void render(@NotNull Gui gui, @NotNull RPlayer player, @NotNull RenderContext context);
    
    void close(@NotNull Gui gui, @NotNull RPlayer player);
}
