package de.t14d3.rapunzellib.commands.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.CommandFeatureInstaller;
import de.t14d3.rapunzellib.commands.CommandFeatureInstallerSupport;
import de.t14d3.rapunzellib.commands.CommandSourceAdapter;
import de.t14d3.rapunzellib.commands.CommandSourceAdapters;
import de.t14d3.rapunzellib.commands.RCommandService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.plugin.PluginContainer;

import java.util.List;

public final class SpongeCommandFeatureInstaller implements CommandFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.SPONGE;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        CommandSourceAdapter adapter = CommandFeatureInstallerSupport.commandSourceAdapter(
            platformId(),
            source -> true,
            SpongeCommandSourceAdapter::wrap
        );

        RCommandService commandService = CommandFeatureInstallerSupport.registerCommandServices(
            context,
            platformId(),
            List.of(adapter)
        );

        CommandSourceAdapters adapters = context.services().get(CommandSourceAdapters.class);
        SpongeCommandRegistrationSupport registrationSupport = context.register(
            SpongeCommandRegistrationSupport.class,
            new SpongeCommandRegistrationSupport(commandService, adapters)
        );

        PluginContainer plugin = context.requireLifecycleOwner(PluginContainer.class);
        SpongeCommandRegistrationBridge bridge = context.register(
            SpongeCommandRegistrationBridge.class,
            new SpongeCommandRegistrationBridge(plugin, registrationSupport)
        );
        bridge.register();
    }
}
