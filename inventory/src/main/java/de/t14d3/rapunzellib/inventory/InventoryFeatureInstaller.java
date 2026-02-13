package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

public interface InventoryFeatureInstaller {
    @NotNull PlatformId platformId();

    void install(@NotNull RapunzelContext context);
}
