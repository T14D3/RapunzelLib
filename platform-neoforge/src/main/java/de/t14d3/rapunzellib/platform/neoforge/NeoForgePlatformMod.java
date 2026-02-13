package de.t14d3.rapunzellib.platform.neoforge;

import de.t14d3.rapunzellib.platform.neoforge.network.NeoForgePluginMessenger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(NeoForgeRapunzelBootstrap.MOD_ID)
public final class NeoForgePlatformMod {
    public NeoForgePlatformMod(IEventBus modEventBus) {
        NeoForgePlatformBootstrapHost host = NeoForgePlatformBootstrapHost.registerCanonicalHost();
        modEventBus.addListener(NeoForgePluginMessenger::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(host::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(host::onServerStopping);
        NeoForge.EVENT_BUS.addListener(host::onServerStopped);
    }
}
