package de.t14d3.rapunzellib.gui.context;

import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DropdownContext {
    @NotNull RPlayer player();
    
    @NotNull String key();
    
    @NotNull String selectedId();
    
    @Nullable Option selectedOption();
}
