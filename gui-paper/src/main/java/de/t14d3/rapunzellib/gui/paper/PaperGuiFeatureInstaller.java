package de.t14d3.rapunzellib.gui.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportManifests;
import de.t14d3.rapunzellib.gui.AbstractGuiFeatureInstaller;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.paper.inventory.InventoryClickListener;
import de.t14d3.rapunzellib.gui.paper.inventory.InventoryRenderer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class PaperGuiFeatureInstaller extends AbstractGuiFeatureInstaller {
    private JavaPlugin plugin;
    private InventoryClickListener listener;

    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public @NotNull GameEventSupportManifest supportManifest() {
        return GameEventSupportManifests.partialGuiInventoryBridgeSupport(platformId());
    }

    @Override
    protected @NotNull GuiRenderer createRenderer(@NotNull RapunzelContext context) {
        plugin = context.requireLifecycleOwner(JavaPlugin.class);
        listener = new InventoryClickListener(plugin, InventoryRenderer.instance());
        listener.register();
        return PaperGuiRenderer.auto();
    }

    @Override
    protected void afterRegister(@NotNull RapunzelContext context, @NotNull GuiRenderer renderer) {
        PaperGuiProvider.setInstalledState(plugin, listener);
    }
}
