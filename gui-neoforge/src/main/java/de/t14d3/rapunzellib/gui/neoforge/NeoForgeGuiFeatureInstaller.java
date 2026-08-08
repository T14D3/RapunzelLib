package de.t14d3.rapunzellib.gui.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.gui.AbstractGuiFeatureInstaller;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.shared.map.SharedMapGuiRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class NeoForgeGuiFeatureInstaller extends AbstractGuiFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }

    @Override
    public @NotNull Collection<GuiRenderer> provideRenderers(@NotNull RapunzelContext context) {
        return List.of(
            NeoForgeGuiRenderer.inventory(),
            NeoForgeGuiRenderer.dialog(),
            NeoForgeGuiRenderer.auto(),
            SharedMapGuiRenderer.INSTANCE
        );
    }

    @Override
    protected @NotNull GuiRenderer createRenderer(@NotNull RapunzelContext context) {
        return NeoForgeGuiRenderer.auto();
    }
}
