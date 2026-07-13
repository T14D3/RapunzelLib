package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A renderer capable of displaying a {@link Gui} to a player.
 * <p>
 * Each renderer advertises a set of {@link GuiCapability capabilities} and
 * handles the {@link #render(Gui, RPlayer, RenderContext)} and
 * {@link #close(Gui, RPlayer)} lifecycle.
 * </p>
 */
public interface GuiRenderer {
    
    @NotNull String name();

    @NotNull Set<GuiCapability> capabilities();

    /**
     * Checks whether this renderer supports a specific capability.
     *
     * @param capability the capability to check
     * @return true if supported
     */
    boolean supports(@NotNull GuiCapability capability);

    /**
     * Renders the given GUI for the player within the provided context.
     *
     * @param gui     the GUI to render
     * @param player  the target player
     * @param context the render context with capability-specific settings
     */
    void render(@NotNull Gui gui, @NotNull RPlayer player, @NotNull RenderContext context);

    /**
     * Closes the given GUI for the player.
     *
     * @param gui    the GUI to close
     * @param player the target player
     */
    void close(@NotNull Gui gui, @NotNull RPlayer player);
}
