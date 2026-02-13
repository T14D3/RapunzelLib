package de.t14d3.rapunzellib.gui.context;

import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

public interface InputContext {
    @NotNull RPlayer player();
    
    @NotNull String key();
    
    @NotNull String value();
}
