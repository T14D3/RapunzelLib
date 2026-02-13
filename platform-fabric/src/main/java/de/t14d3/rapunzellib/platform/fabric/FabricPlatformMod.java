package de.t14d3.rapunzellib.platform.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class FabricPlatformMod implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricPlatformBootstrapHost host = FabricPlatformBootstrapHost.registerCanonicalHost();
        ServerLifecycleEvents.SERVER_STARTING.register(host::bindServer);
        ServerLifecycleEvents.SERVER_STOPPING.register(host::onServerStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(host::onServerStopped);
    }
}
