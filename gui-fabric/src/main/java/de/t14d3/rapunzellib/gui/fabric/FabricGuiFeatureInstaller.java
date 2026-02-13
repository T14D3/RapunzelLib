package de.t14d3.rapunzellib.gui.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportManifests;
import de.t14d3.rapunzellib.gui.AbstractGuiFeatureInstaller;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import org.jetbrains.annotations.NotNull;

public final class FabricGuiFeatureInstaller extends AbstractGuiFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.FABRIC;
    }

    @Override
    public @NotNull GameEventSupportManifest supportManifest() {
        return GameEventSupportManifests.partialGuiInventoryBridgeSupport(platformId());
    }

    @Override
    protected @NotNull GuiRenderer createRenderer(@NotNull RapunzelContext context) {
        return FabricGuiRenderer.auto();
    }
}
