package de.t14d3.rapunzellib.livetest.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.CommandFeatures;
import de.t14d3.rapunzellib.commands.RCommandService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.livetest.BotService;
import de.t14d3.rapunzellib.livetest.RpcBotService;
import de.t14d3.rapunzellib.livetest.shared.AbstractSharedLiveTestFeatureInstaller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * NeoForge-specific implementation of {@link de.t14d3.rapunzellib.livetest.LiveTestFeatureInstaller}.
 * <p>
 * Registers the live test host, bot service (when configured), and the
 * {@code /livetest} command via RapunzelLib's RLib command system.
 * Bot support uses the TCP-based {@link RpcBotService} when the system
 * property {@code rapunzellib.bot.rpc.port} is set.
 * </p>
 */
public final class NeoForgeLiveTestFeatureInstaller extends AbstractSharedLiveTestFeatureInstaller {

    private static final String RPC_PORT_PROPERTY = "rapunzellib.bot.rpc.port";

    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }

    @Override
    protected @Nullable BotService createBotService(@NotNull RapunzelContext context) {
        String portStr = System.getProperty(RPC_PORT_PROPERTY);
        if (portStr != null && !portStr.isBlank()) {
            try {
                int port = Integer.parseInt(portStr);
                context.logger().info("[LIVETEST] Connecting to bot TCP server on port {}", port);
                return new RpcBotService("127.0.0.1", port);
            } catch (Exception e) {
                context.logger().warn("[LIVETEST] Failed to connect to bot TCP server: {}", e.getMessage());
            }
        }
        return null;
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
