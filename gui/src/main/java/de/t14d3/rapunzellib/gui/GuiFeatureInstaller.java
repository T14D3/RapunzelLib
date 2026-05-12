package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventSupportContributor;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportManifests;
import org.jetbrains.annotations.NotNull;

/**
 * Platform-specific installer for GUI features.
 * <p>
 * Implementations provide a {@link PlatformId} and install all necessary
 * GUI support for that platform when {@link #install(RapunzelContext)} is called.
 * Also contributes game event support manifests via {@link GameEventSupportContributor}.
 * </p>
 */
public interface GuiFeatureInstaller extends GameEventSupportContributor {
    @NotNull PlatformId platformId();

    @Override
    default @NotNull GameEventSupportManifest supportManifest() {
        return GameEventSupportManifests.guiInventoryBridgeSupport(platformId());
    }

    void install(@NotNull RapunzelContext context);
}
