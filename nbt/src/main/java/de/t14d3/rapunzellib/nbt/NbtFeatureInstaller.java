package de.t14d3.rapunzellib.nbt;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

public interface NbtFeatureInstaller {
    @NotNull PlatformId platformId();

    void install(@NotNull RapunzelContext context);
}
