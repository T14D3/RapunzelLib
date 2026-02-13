package de.t14d3.rapunzellib.gui.context;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

public interface CloseContext {
    @NotNull RPlayer player();
    
    @NotNull Gui gui();
    
    @NotNull CloseReason reason();
}
