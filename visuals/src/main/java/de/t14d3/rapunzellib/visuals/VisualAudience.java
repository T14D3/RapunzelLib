package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Defines the target audience for a visual.
 * <p>
 * Implementations resolve to a collection of {@link RPlayer players}
 * that should receive the visual's packets. Static factory methods
 * provide common audience types.
 */
public interface VisualAudience {

    /**
     * Resolves this audience to the collection of target players.
     *
     * @return the collection of players in this audience
     */
    @NotNull Collection<RPlayer> resolve();

    static @NotNull VisualAudience empty() {
        return Collections::emptyList;
    }

    static @NotNull VisualAudience player(@NotNull RPlayer player) {
        return () -> Collections.singletonList(player);
    }

    /**
     * Returns an audience targeting a specific collection of players.
     *
     * @param players the target players
     * @return a multi-player audience
     */
    static @NotNull VisualAudience players(@NotNull Collection<? extends RPlayer> players) {
        return () -> Collections.unmodifiableCollection(new ArrayList<>(players));
    }

    /**
     * Returns an audience targeting all online players within range of a center location.
     *
     * @param center the center location
     * @param radius the search radius in blocks
     * @return a range-based audience
     */
    static @NotNull VisualAudience allInRange(@NotNull RLocation center, double radius) {
        return new RangeVisualAudience(center, radius);
    }
}
