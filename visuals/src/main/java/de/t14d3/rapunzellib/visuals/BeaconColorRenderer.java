package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RBlockPos;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface BeaconColorRenderer {
    @NotNull NamedTextColor render(@NotNull RBlockPos glassPos, int index, int totalHeight);
}
