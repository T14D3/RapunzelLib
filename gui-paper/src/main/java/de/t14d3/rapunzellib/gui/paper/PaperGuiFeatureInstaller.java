package de.t14d3.rapunzellib.gui.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportManifests;
import de.t14d3.rapunzellib.gui.AbstractGuiFeatureInstaller;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.shared.map.SharedMapGuiRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class PaperGuiFeatureInstaller extends AbstractGuiFeatureInstaller {

    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public @NotNull GameEventSupportManifest supportManifest() {
        return GameEventSupportManifests.partialGuiInventoryBridgeSupport(platformId());
    }

    @Override
    public @NotNull Collection<GuiRenderer> provideRenderers(@NotNull RapunzelContext context) {
        return List.of(
            PaperGuiRenderer.inventory(),
            PaperGuiRenderer.dialog(),
            PaperGuiRenderer.auto(),
            SharedMapGuiRenderer.INSTANCE
        );
    }

    @Override
    protected @NotNull GuiRenderer createRenderer(@NotNull RapunzelContext context) {
        return PaperGuiRenderer.auto();
    }
}
