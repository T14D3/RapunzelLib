package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.gui.builder.GuiBuilder;
import de.t14d3.rapunzellib.gui.layout.GuiLayout;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Gui {
    @NotNull GuiRenderer renderer();
    
    @NotNull GuiLayout layout();
    
    /**
     * Get the title of this GUI.
     * @return the title component, or null if not set
     */
    @Nullable Component title();
    
    /**
     * Get the number of rows for grid-based layouts.
     * @return the number of rows (1-6), or 0 if using linear layout
     */
    int rows();
    
    void open(@NotNull RPlayer player);
    
    void close(@NotNull RPlayer player);
    
    @NotNull
    static GuiBuilder builder() {
        return new GuiBuilder();
    }
}
