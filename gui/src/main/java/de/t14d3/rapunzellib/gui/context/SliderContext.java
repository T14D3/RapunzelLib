package de.t14d3.rapunzellib.gui.context;

import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

public interface SliderContext {
    @NotNull RPlayer player();
    
    @NotNull String key();
    
    float value();
    
    default int intValue() {
        return Math.round(value());
    }
    
    default double doubleValue() {
        return value();
    }
}
