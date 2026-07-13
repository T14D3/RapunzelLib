package de.t14d3.rapunzellib.livetest.paper;

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
 * Paper-specific implementation of {@link de.t14d3.rapunzellib.livetest.LiveTestFeatureInstaller}.
 * <p>
 * Registers the live test host, bot service, and the {@code /livetest} and
 * {@code /botcallback} commands via RapunzelLib's RLib command system.
 * Uses the TCP-based {@link RpcBotService} when the system property
 * {@code rapunzellib.bot.rpc.port} is set, otherwise falls back to the
 * console-based stdout protocol.
 * </p>
 */
public final class PaperLiveTestFeatureInstaller extends AbstractSharedLiveTestFeatureInstaller {

    private static final String RPC_PORT_PROPERTY = "rapunzellib.bot.rpc.port";

    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
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
                context.logger().warn("[LIVETEST] Failed to connect to bot TCP server, falling back to stdout: {}", e.getMessage());
            }
        }
        return null; // Use console-based fallback
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

            // Register /botcallback command via RLib
            commandService.registerRoot("rapunzellib-botcallback", createBotCallbackNode());

        } catch (Exception e) {
            context.logger().error("Failed to register livetest commands via RLib", e);
        }
    }
}
