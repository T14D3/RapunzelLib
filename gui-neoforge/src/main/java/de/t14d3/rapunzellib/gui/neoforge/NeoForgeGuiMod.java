package de.t14d3.rapunzellib.gui.neoforge;

import de.t14d3.rapunzellib.gui.neoforge.dialog.DialogPacketHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(NeoForgeGuiMod.MOD_ID)
public final class NeoForgeGuiMod {
    public static final String MOD_ID = "rapunzellib_gui_neoforge";

    public NeoForgeGuiMod(IEventBus modEventBus) {
        modEventBus.addListener(DialogPacketHandler::registerPayloadHandlers);
    }
}
