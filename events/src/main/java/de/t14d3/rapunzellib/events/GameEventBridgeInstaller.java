package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface GameEventBridgeInstaller {
    @NotNull PlatformId platformId();

    /**
     * Installs platform hooks and connects them to {@code bus}.
     *
     * @param owner platform-specific owner object (e.g. a Bukkit JavaPlugin), may be {@code null} for platforms that
     *              do not need an owner token.
     */
    @NotNull GameEventBridge install(@NotNull RapunzelContext context, @NotNull GameEventBus bus, @Nullable Object owner);
}
