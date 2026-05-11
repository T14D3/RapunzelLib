package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public interface VisualAudience {
    @NotNull Collection<RPlayer> resolve();

    static @NotNull VisualAudience empty() {
        return Collections::emptyList;
    }

    static @NotNull VisualAudience player(@NotNull RPlayer player) {
        return () -> Collections.singletonList(player);
    }

    static @NotNull VisualAudience players(@NotNull Collection<? extends RPlayer> players) {
        return () -> Collections.unmodifiableCollection(new ArrayList<>(players));
    }

    static @NotNull VisualAudience allInRange(@NotNull RLocation center, double radius) {
        return new RangeVisualAudience(center, radius);
    }
}
