package de.t14d3.rapunzellib.platform.neoforge;

import de.t14d3.rapunzellib.platform.neoforge.network.NeoForgePluginMessenger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(NeoForgeRapunzelBootstrap.MOD_ID)
public final class NeoForgePlatformMod {
    public NeoForgePlatformMod(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgePluginMessenger::registerPayloadHandlers);
    }
}
