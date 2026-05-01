package de.t14d3.rapunzellib.commands.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.CommandFeatureInstaller;
import de.t14d3.rapunzellib.commands.CommandFeatureInstallerSupport;
import de.t14d3.rapunzellib.commands.CommandSourceAdapter;
import de.t14d3.rapunzellib.commands.CommandSourceAdapters;
import de.t14d3.rapunzellib.commands.RCommandService;
import de.t14d3.rapunzellib.commands.SharedBrigadierCommandRegistrar;
import de.t14d3.rapunzellib.commands.SharedCommandFeatureInstallerSupport;
import de.t14d3.rapunzellib.context.RapunzelContext;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FabricCommandFeatureInstaller implements CommandFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.FABRIC;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        CommandSourceAdapter adapter = CommandFeatureInstallerSupport.commandSourceAdapter(
            platformId(),
            CommandSourceStack.class,
            FabricCommandSourceAdapter::wrap
        );

        RCommandService commandService = CommandFeatureInstallerSupport.registerCommandServices(
            context,
            platformId(),
            List.of(adapter)
        );
        CommandSourceAdapters adapters = context.services().get(CommandSourceAdapters.class);
        context.register(
            SharedBrigadierCommandRegistrar.class,
            new FabricSharedBrigadierCommandRegistrar(commandService, adapters)
        );
        SharedCommandFeatureInstallerSupport.installRuntimeCommandRegistrationSupport(context);
    }
}
