package de.t14d3.rapunzellib.events.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBridgeInstaller;
import de.t14d3.rapunzellib.events.GameEventBus;
import org.jetbrains.annotations.NotNull;

public final class SpongeGameEventBridgeInstaller implements GameEventBridgeInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.SPONGE;
    }

    @Override
    public @NotNull GameEventBridge install(@NotNull RapunzelContext context, @NotNull GameEventBus bus, Object owner) {
        SpongeGameEventsBridge bridge = new SpongeGameEventsBridge(bus);
        bridge.register(owner);
        return bridge;
    }
}
