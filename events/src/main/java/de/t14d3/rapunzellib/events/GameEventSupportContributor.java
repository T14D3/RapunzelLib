package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

public interface GameEventSupportContributor {
    @NotNull PlatformId platformId();

    default @NotNull GameEventSupportManifest supportManifest() {
        return GameEventSupportManifest.empty(platformId());
    }
}
