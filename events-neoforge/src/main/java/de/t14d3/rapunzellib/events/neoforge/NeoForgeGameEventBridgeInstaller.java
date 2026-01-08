package de.t14d3.rapunzellib.events.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBridgeInstaller;
import de.t14d3.rapunzellib.events.GameEventBus;

public final class NeoForgeGameEventBridgeInstaller implements GameEventBridgeInstaller {
    @Override
    public PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }

    @Override
    public GameEventBridge install(RapunzelContext context, GameEventBus bus, Object owner) {
        NeoForgeGameEventsBridge bridge = new NeoForgeGameEventsBridge(bus);
        bridge.register();
        return bridge;
    }
}

