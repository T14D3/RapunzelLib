package de.t14d3.rapunzellib.commands.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.AbstractSharedBrigadierCommandEntrypoint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class FabricCommandsMod extends AbstractSharedBrigadierCommandEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerSharedCommands(dispatcher));
    }

    @Override
    protected PlatformId platformId() {
        return PlatformId.FABRIC;
    }
}
