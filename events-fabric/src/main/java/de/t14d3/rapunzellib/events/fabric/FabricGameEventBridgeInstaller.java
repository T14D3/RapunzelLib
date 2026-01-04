package de.t14d3.rapunzellib.events.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBridgeInstaller;
import de.t14d3.rapunzellib.events.GameEventBus;

public final class FabricGameEventBridgeInstaller implements GameEventBridgeInstaller {
    @Override
    public PlatformId platformId() {
        return PlatformId.FABRIC;
    }

    @Override
    public GameEventBridge install(RapunzelContext context, GameEventBus bus, Object owner) {
        FabricGameEventsBridge bridge = new FabricGameEventsBridge(bus);
        bridge.register();
        return bridge;
    }
}
