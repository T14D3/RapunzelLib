package de.t14d3.rapunzellib.events.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.AbstractGameEventBridgeInstaller;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.player.PlayerMoveThrottle;
import org.jetbrains.annotations.NotNull;

public final class FabricGameEventBridgeInstaller extends AbstractGameEventBridgeInstaller {
    public FabricGameEventBridgeInstaller() {
        super(PlatformId.FABRIC, FabricGameEventSupport.MANIFEST);
    }

    @Override
    protected @NotNull GameEventBridge installBridge(@NotNull RapunzelContext context, @NotNull GameEventBus bus) {
        PlayerMoveThrottle.load(context);
        return FabricGameEventsBridge.install(bus);
    }
}
