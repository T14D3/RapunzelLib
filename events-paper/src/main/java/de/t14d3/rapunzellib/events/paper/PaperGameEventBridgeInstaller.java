package de.t14d3.rapunzellib.events.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBridgeInstaller;
import de.t14d3.rapunzellib.events.GameEventBus;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperGameEventBridgeInstaller implements GameEventBridgeInstaller {
    @Override
    public PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public GameEventBridge install(RapunzelContext context, GameEventBus bus, Object owner) {
        if (!(owner instanceof JavaPlugin plugin)) {
            throw new IllegalArgumentException(
                "Paper event bridge requires a JavaPlugin owner (pass your plugin instance to GameEvents.install(plugin))"
            );
        }
        PaperGameEventsBridge bridge = new PaperGameEventsBridge(plugin, bus);  
        bridge.register();
        return bridge;
    }
}
