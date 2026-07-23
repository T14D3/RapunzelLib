package de.t14d3.rapunzellib.livetest.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.CommandFeatures;
import de.t14d3.rapunzellib.commands.RCommandService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.livetest.shared.AbstractSharedLiveTestFeatureInstaller;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * NeoForge-specific implementation of {@link de.t14d3.rapunzellib.livetest.LiveTestFeatureInstaller}.
 * <p>
 * Registers the live test host and the {@code /livetest} command via
 * RapunzelLib's RLib command system. Bot support is left unregistered
 * on NeoForge for now.
 * </p>
 */
public final class NeoForgeLiveTestFeatureInstaller extends AbstractSharedLiveTestFeatureInstaller {

    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }

    @Override
    protected void registerCommands(@NotNull RapunzelContext context) {
        Objects.requireNonNull(context, "context");
        try {
            // Obtain or install the RLib command service
            RCommandService commandService = context.services()
                    .find(RCommandService.class)
                    .orElseGet(() -> CommandFeatures.install(context));

            // Register /livetest command via RLib
            commandService.registerRoot("rapunzellib-livetest", createLivetestNode(context));

        } catch (Exception e) {
            context.logger().error("Failed to register livetest commands via RLib", e);
        }
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        Objects.requireNonNull(context, "context");
        super.install(context);
    }
}
