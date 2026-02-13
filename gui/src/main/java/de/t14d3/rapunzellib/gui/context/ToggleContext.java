package de.t14d3.rapunzellib.gui.context;

import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ToggleContext {
    @NotNull RPlayer player();
    
    @NotNull String key();
    
    boolean value();
}
