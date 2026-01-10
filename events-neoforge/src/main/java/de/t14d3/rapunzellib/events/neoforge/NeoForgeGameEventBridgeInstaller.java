package de.t14d3.rapunzellib.events.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBridgeInstaller;
import de.t14d3.rapunzellib.events.GameEventBus;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeGameEventBridgeInstaller implements GameEventBridgeInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }

    @Override
    public @NotNull GameEventBridge install(@NotNull RapunzelContext context, @NotNull GameEventBus bus, Object owner) {
        NeoForgeGameEventsBridge bridge = new NeoForgeGameEventsBridge(bus);
        bridge.register();
        return bridge;
    }
}

