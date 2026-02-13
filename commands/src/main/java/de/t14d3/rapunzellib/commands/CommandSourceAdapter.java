package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

public interface CommandSourceAdapter {
    @NotNull PlatformId platformId();

    boolean supports(@NotNull Object source);

    @NotNull RCommandSource wrap(@NotNull Object source);
}
