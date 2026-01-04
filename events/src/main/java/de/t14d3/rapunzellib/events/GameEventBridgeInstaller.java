package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;

public interface GameEventBridgeInstaller {
    PlatformId platformId();

    /**
     * Installs platform hooks and connects them to {@code bus}.
     *
     * @param owner platform-specific owner object (e.g. a Bukkit JavaPlugin), may be {@code null} for platforms that
     *              do not need an owner token.
     */
    GameEventBridge install(RapunzelContext context, GameEventBus bus, Object owner);
}
