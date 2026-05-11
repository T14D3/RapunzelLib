package de.t14d3.rapunzellib.visuals;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public interface BlockStructureVisual extends Visual<BlockStructureConfig> {
    void updateColor(@NotNull TextColor color);
}
