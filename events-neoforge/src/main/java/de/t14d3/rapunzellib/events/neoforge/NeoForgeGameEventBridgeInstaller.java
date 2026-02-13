package de.t14d3.rapunzellib.events.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.AbstractGameEventBridgeInstaller;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeGameEventBridgeInstaller extends AbstractGameEventBridgeInstaller {
    public NeoForgeGameEventBridgeInstaller() {
        super(PlatformId.NEOFORGE, NeoForgeGameEventSupport.MANIFEST);
    }

    @Override
    protected @NotNull GameEventBridge installBridge(@NotNull RapunzelContext context, @NotNull GameEventBus bus) {
        return NeoForgeGameEventsBridge.install(bus);
    }
}
