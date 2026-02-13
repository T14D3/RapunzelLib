package de.t14d3.rapunzellib.gui.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportManifests;
import de.t14d3.rapunzellib.gui.AbstractGuiFeatureInstaller;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import org.jetbrains.annotations.NotNull;

public final class SpongeGuiFeatureInstaller extends AbstractGuiFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.SPONGE;
    }

    @Override
    public @NotNull GameEventSupportManifest supportManifest() {
        return GameEventSupportManifests.partialGuiInventoryBridgeSupport(platformId());
    }

    @Override
    protected @NotNull GuiRenderer createRenderer(@NotNull RapunzelContext context) {
        return SpongeGuiRenderer.auto();
    }
}
