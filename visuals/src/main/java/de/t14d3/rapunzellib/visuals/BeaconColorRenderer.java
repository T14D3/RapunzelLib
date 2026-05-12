package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RBlockPos;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for custom beacon beam color rendering.
 * <p>
 * Implementations determine the color of each glass block in the
 * beacon beam column, given its position, index, and total height.
 */
@FunctionalInterface
public interface BeaconColorRenderer {

    /**
     * Renders the color for a single glass block in the beacon column.
     *
     * @param glassPos    the position of the glass block
     * @param index       the zero-based index of this block in the column
     * @param totalHeight the total height of the glass column
     * @return the named text color for this glass block
     */
    @NotNull NamedTextColor render(@NotNull RBlockPos glassPos, int index, int totalHeight);
}
