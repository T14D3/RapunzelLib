package de.t14d3.rapunzellib.visuals.fabric;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.visuals.VisualManager;
import de.t14d3.rapunzellib.visuals.Visuals;
import org.jetbrains.annotations.NotNull;

public final class FabricVisuals implements Visuals {
    private final FabricVisualManager manager;

    public FabricVisuals(@NotNull RapunzelContext context) {
        this.manager = new FabricVisualManager(context);
    }

    @Override
    public @NotNull VisualManager manager() {
        return manager;
    }
}
