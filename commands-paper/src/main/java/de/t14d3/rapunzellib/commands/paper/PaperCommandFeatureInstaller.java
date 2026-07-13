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
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public final class PaperCommandFeatureInstaller implements CommandFeatureInstaller {
    private static final Logger logger = LoggerFactory.getLogger(PaperCommandFeatureInstaller.class);

    // Deferred command registration callback - set by install(), consumed by the
    // lifecycle handler registered in PaperPlatformPlugin.onLoad().
    static volatile Consumer<io.papermc.paper.command.brigadier.Commands> deferredRegistration;

    /**
     * Registers the Paper lifecycle command handler.
     * Must be called from {@code onLoad()} (when lifecycle state is NONE).
     */
    public static void registerLifecycleHandler(@NotNull JavaPlugin plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Consumer<io.papermc.paper.command.brigadier.Commands> reg = deferredRegistration;
            if (reg != null) {
                reg.accept(event.registrar());
            } else {
                logger.info("Deferred command registration not yet available; skipping lifecycle event.");
            }
        });
    }

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

        // Set the deferred registration callback for the lifecycle handler
        // (registered in onLoad()). This runs when the COMMANDS lifecycle event fires.
        deferredRegistration = commands -> runtimeSupport.sync(
            commands.getDispatcher(),
            () -> PaperSharedBrigadierRegistration.register(commands, commandService, adapters)
        );
    }
}
