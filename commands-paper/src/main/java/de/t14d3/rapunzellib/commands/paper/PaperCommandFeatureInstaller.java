package de.t14d3.rapunzellib.commands.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.CommandFeatureInstaller;
import de.t14d3.rapunzellib.commands.CommandFeatureInstallerSupport;
import de.t14d3.rapunzellib.commands.CommandSourceAdapter;
import de.t14d3.rapunzellib.commands.CommandSourceAdapters;
import de.t14d3.rapunzellib.commands.SharedCommandFeatureInstallerSupport;
import de.t14d3.rapunzellib.commands.SharedRuntimeCommandRegistrationSupport;
import de.t14d3.rapunzellib.commands.RCommandService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PaperCommandFeatureInstaller implements CommandFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        CommandSourceAdapter brigadierAdapter = CommandFeatureInstallerSupport.commandSourceAdapter(
            platformId(),
            CommandSourceStack.class,
            PaperBrigadierCommandSourceAdapter::wrap
        );
        CommandSourceAdapter senderAdapter = CommandFeatureInstallerSupport.commandSourceAdapter(
            platformId(),
            CommandSender.class,
            PaperCommandSourceAdapter::wrap
        );

        RCommandService commandService = CommandFeatureInstallerSupport.registerSharedBrigadierCommandServices(
            context,
            platformId(),
            List.of(brigadierAdapter, senderAdapter),
            CommandSourceStack.class
        );

        CommandSourceAdapters adapters = context.services().get(CommandSourceAdapters.class);
        SharedRuntimeCommandRegistrationSupport runtimeSupport =
            SharedCommandFeatureInstallerSupport.installRuntimeCommandRegistrationSupport(context);

        JavaPlugin plugin = context.requireLifecycleOwner(JavaPlugin.class);
        PaperLifecycleCommandRegistrationSupport.register(
            plugin.getLifecycleManager(),
            event -> runtimeSupport.sync(
                event.registrar().getDispatcher(),
                () -> PaperSharedBrigadierRegistration.register(event.registrar(), commandService, adapters)
            )
        );
    }
}
