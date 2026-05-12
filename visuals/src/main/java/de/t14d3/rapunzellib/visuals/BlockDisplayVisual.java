package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.registry.RBlockType;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * A visual that renders a block display entity at a specific location.
 * Supports dynamic updates to the transform, block type, and color.
 */
public interface BlockDisplayVisual extends Visual<BlockDisplayConfig> {

    /**
     * Updates the display transformation (translation, scale, rotation).
     *
     * @param transform the new transform
     */
    void updateTransform(@NotNull DisplayTransform transform);

    /**
     * Updates the displayed block type.
     *
     * @param block the new block type
     */
    void updateBlock(@NotNull RBlockType block);

    /**
     * Updates the overlay color.
     *
     * @param color the new text color
     */
    void updateColor(@NotNull TextColor color);
}
