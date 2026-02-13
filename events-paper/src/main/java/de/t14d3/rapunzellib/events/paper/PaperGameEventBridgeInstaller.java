package de.t14d3.rapunzellib.events.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.LifecycleOwnerGameEventBridgeInstaller;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class PaperGameEventBridgeInstaller extends LifecycleOwnerGameEventBridgeInstaller {
    public PaperGameEventBridgeInstaller() {
        super(PlatformId.PAPER, PaperGameEventSupport.MANIFEST, "org.bukkit.plugin.java.JavaPlugin");
    }

    @Override
    protected @NotNull GameEventBridge installBridge(
        @NotNull RapunzelContext context,
        @NotNull GameEventBus bus,
        @NotNull Object owner
    ) {
        JavaPlugin plugin = (JavaPlugin) owner;
        PaperGameEventsBridge bridge = new PaperGameEventsBridge(plugin, bus);
        bridge.register();
        return bridge;
    }
}
