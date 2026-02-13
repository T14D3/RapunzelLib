package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

public interface GameEventBridgeInstaller {
    @NotNull PlatformId platformId();

    default @NotNull GameEventSupportManifest supportManifest() {
        return GameEventSupportManifest.empty(platformId());
    }

    /**
     * Installs platform hooks and connects them to {@code bus}.
     */
    @NotNull GameEventBridge install(@NotNull RapunzelContext context, @NotNull GameEventBus bus);
}
