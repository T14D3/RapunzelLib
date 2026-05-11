package de.t14d3.rapunzellib.visuals.neoforge;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.visuals.VisualManager;
import de.t14d3.rapunzellib.visuals.Visuals;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeVisuals implements Visuals {
    private final NeoForgeVisualManager manager;

    public NeoForgeVisuals(@NotNull RapunzelContext context) {
        this.manager = new NeoForgeVisualManager(context);
    }

    @Override
    public @NotNull VisualManager manager() {
        return manager;
    }
}
