package de.t14d3.rapunzellib.events.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.LifecycleOwnerGameEventBridgeInstaller;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.plugin.PluginContainer;

public final class SpongeGameEventBridgeInstaller extends LifecycleOwnerGameEventBridgeInstaller {
    public SpongeGameEventBridgeInstaller() {
        super(PlatformId.SPONGE, SpongeGameEventSupport.MANIFEST, "org.spongepowered.plugin.PluginContainer");
    }

    @Override
    protected @NotNull GameEventBridge installBridge(
        @NotNull RapunzelContext context,
        @NotNull GameEventBus bus,
        @NotNull Object owner
    ) {
        PluginContainer plugin = (PluginContainer) owner;
        SpongeGameEventsBridge bridge = new SpongeGameEventsBridge(bus);
        bridge.register(plugin);
        return bridge;
    }
}
