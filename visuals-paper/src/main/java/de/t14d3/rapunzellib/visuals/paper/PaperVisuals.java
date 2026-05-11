package de.t14d3.rapunzellib.visuals.paper;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.visuals.VisualManager;
import de.t14d3.rapunzellib.visuals.Visuals;
import org.jetbrains.annotations.NotNull;

public final class PaperVisuals implements Visuals {
    private final PaperVisualManager manager;

    public PaperVisuals(@NotNull RapunzelContext context) {
        this.manager = new PaperVisualManager(context);
    }

    @Override
    public @NotNull VisualManager manager() {
        return manager;
    }
}
