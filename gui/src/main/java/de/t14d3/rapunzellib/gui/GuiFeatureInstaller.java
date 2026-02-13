package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventSupportContributor;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportManifests;
import org.jetbrains.annotations.NotNull;

public interface GuiFeatureInstaller extends GameEventSupportContributor {
    @NotNull PlatformId platformId();

    @Override
    default @NotNull GameEventSupportManifest supportManifest() {
        return GameEventSupportManifests.guiInventoryBridgeSupport(platformId());
    }

    void install(@NotNull RapunzelContext context);
}
