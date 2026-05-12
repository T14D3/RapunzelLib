package de.t14d3.rapunzellib.visuals;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * A visual that renders a block structure (box or wall) as block display entities.
 */
public interface BlockStructureVisual extends Visual<BlockStructureConfig> {

    /**
     * Updates the color of all blocks in the structure.
     *
     * @param color the new text color
     */
    void updateColor(@NotNull TextColor color);
}
