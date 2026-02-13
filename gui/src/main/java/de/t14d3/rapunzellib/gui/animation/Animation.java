package de.t14d3.rapunzellib.gui.animation;

import de.t14d3.rapunzellib.gui.Gui;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public interface Animation {
    @NotNull Duration interval();
    
    void tick(@NotNull Gui gui, int frame);
    
    default boolean isComplete(int frame) {
        return false;
    }
}
