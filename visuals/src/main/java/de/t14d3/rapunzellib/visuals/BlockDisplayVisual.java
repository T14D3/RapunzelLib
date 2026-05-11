package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.registry.RBlockType;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public interface BlockDisplayVisual extends Visual<BlockDisplayConfig> {
    void updateTransform(@NotNull DisplayTransform transform);

    void updateBlock(@NotNull RBlockType block);

    void updateColor(@NotNull TextColor color);
}
