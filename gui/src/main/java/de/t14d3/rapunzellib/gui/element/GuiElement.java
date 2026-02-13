package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.RenderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface GuiElement {
    @NotNull ElementType type();
    
    default int width() {
        return -1;
    }
    
    default int height() {
        return -1;
    }
    
    default @Nullable Consumer<RenderContext> renderer() {
        return null;
    }
}
