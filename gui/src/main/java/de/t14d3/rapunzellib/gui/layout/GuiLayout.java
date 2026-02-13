package de.t14d3.rapunzellib.gui.layout;

import de.t14d3.rapunzellib.gui.element.GuiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface GuiLayout {
    @NotNull List<GuiElement> elements();
    
    default @NotNull List<GuiElement> elementsAt(int slot) {
        return Collections.emptyList();
    }
}
