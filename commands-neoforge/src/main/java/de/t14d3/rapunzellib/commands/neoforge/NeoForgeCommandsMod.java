package de.t14d3.rapunzellib.commands.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.AbstractSharedBrigadierCommandEntrypoint;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.Mod;

@Mod(NeoForgeCommandsMod.MOD_ID)
public final class NeoForgeCommandsMod extends AbstractSharedBrigadierCommandEntrypoint {
    public static final String MOD_ID = "rapunzellib_commands_neoforge";

    public NeoForgeCommandsMod() {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        registerSharedCommands(event.getDispatcher());
    }

    @Override
    protected PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }
}
