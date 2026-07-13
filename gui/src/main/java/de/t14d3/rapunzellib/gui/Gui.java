package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.gui.builder.GuiBuilder;
import de.t14d3.rapunzellib.gui.layout.GuiLayout;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a GUI screen in the RapunzelLib GUI system.
 * <p>
 * A GUI consists of a renderer, a layout, an optional title, and a row count.
 * GUIs are typically built using {@link GuiBuilder} obtained via {@link #builder()}.
 * </p>
 */
public interface Gui {
    
    @NotNull GuiRenderer renderer();

    /**
     * Gets the layout that defines the arrangement of elements in this GUI.
     *
     * @return the GUI layout
     */
    @NotNull GuiLayout layout();

    @Nullable Component title();

    int rows();

    /**
     * Opens this GUI for the given player.
     *
     * @param player the player to show the GUI to
     */
    void open(@NotNull RPlayer player);

    /**
     * Closes this GUI for the given player.
     *
     * @param player the player whose GUI to close
     */
    void close(@NotNull RPlayer player);

    @NotNull
    static GuiBuilder builder() {
        return new GuiBuilder();
    }
}
