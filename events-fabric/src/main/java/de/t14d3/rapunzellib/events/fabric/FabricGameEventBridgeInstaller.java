package de.t14d3.rapunzellib.events.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBridgeInstaller;
import de.t14d3.rapunzellib.events.GameEventBus;
import org.jetbrains.annotations.NotNull;

public final class FabricGameEventBridgeInstaller implements GameEventBridgeInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.FABRIC;
    }

    @Override
    public @NotNull GameEventBridge install(@NotNull RapunzelContext context, @NotNull GameEventBus bus, Object owner) {
        FabricGameEventsBridge bridge = new FabricGameEventsBridge(bus);
        bridge.register();
        return bridge;
    }
}
