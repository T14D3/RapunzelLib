package de.t14d3.rapunzellib.visuals.sponge;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.visuals.VisualManager;
import de.t14d3.rapunzellib.visuals.Visuals;
import org.jetbrains.annotations.NotNull;

public final class SpongeVisuals implements Visuals {
    private final SpongeVisualManager manager;

    public SpongeVisuals(@NotNull RapunzelContext context) {
        this.manager = new SpongeVisualManager();
    }

    @Override
    public @NotNull VisualManager manager() {
        return manager;
    }
}
