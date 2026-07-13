package de.t14d3.rapunzellib.platform.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.bootstrap.BootstrapHandle;
import de.t14d3.rapunzellib.commands.ConsoleCommandDispatcher;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.common.context.ConsumerView;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.network.bootstrap.BackendTransportBootstrap;
import de.t14d3.rapunzellib.network.bootstrap.SharedBackendBootstrap;
import de.t14d3.rapunzellib.network.queue.NetworkQueueTransportDecorator;
import de.t14d3.rapunzellib.platform.paper.objects.PaperBlocks;
import de.t14d3.rapunzellib.platform.paper.objects.PaperEntities;
import de.t14d3.rapunzellib.platform.paper.objects.PaperPlayers;
import de.t14d3.rapunzellib.platform.paper.objects.PaperWorlds;
import de.t14d3.rapunzellib.platform.PlatformFeatures;
import de.t14d3.rapunzellib.platform.paper.network.PaperPluginMessenger;
import de.t14d3.rapunzellib.platform.paper.scheduler.PaperScheduler;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeProfiles;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.minecraft.server.MinecraftServer;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Bootstraps RapunzelLib on Paper.
 *
 * <p>Two entry points:</p>
 * <ul>
 *   <li>{@link #bootstrapPlatform} - called by {@link PaperPlatformPlugin#onEnable()}
 *       to create the shared platform context (with {@link PlatformFeatures#install})</li>
 *   <li>{@link #acquire} - called by consumer plugins to borrow a consumer-specific
 *       view of the platform context</li>
 * </ul>
 */
public final class PaperRapunzelBootstrap {
    private PaperRapunzelBootstrap() {
    }

    // ── Platform bootstrap (called by PaperPlatformPlugin) ───────────────────

    /**
     * Bootstraps the shared platform context.
     *
     * <p>Called exactly once by {@link PaperPlatformPlugin#onEnable()}.
     * Registers all platform services ({@link PlatformFeatures#install}) and
     * network transport.</p>
     */
    public static BootstrapHandle bootstrapPlatform(JavaPlugin platformPlugin) {
        return Rapunzel.bootstrap(platformPlugin, () -> createContext(platformPlugin));
    }

    // ── Consumer acquire (called by consumer plugins) ────────────────────────

    /**
     * Acquires a consumer-level view of the shared platform context.
     *
     * <p>Returns a {@link BootstrapHandle} backed by a {@link ConsumerView}
     * that wraps the platform's context with the consumer's own logger,
     * data directory, and resource provider. No platform services are
     * re-registered - the consumer shares the platform's
     * {@link de.t14d3.rapunzellib.context.ServiceRegistry}.</p>
     *
     * @param consumerPlugin the consumer plugin instance
     * @return a borrower handle providing a consumer-scoped context
     * @throws IllegalStateException if no platform context exists
     */
    public static BootstrapHandle acquire(JavaPlugin consumerPlugin) {
        RapunzelContext platform = Rapunzel.context();
        ConsumerView view = new ConsumerView(
            platform,
            consumerPlugin.getSLF4JLogger(),
            consumerPlugin.getDataFolder().toPath(),
            path -> Optional.ofNullable(openResource(consumerPlugin, path)),
            new LifecycleOwner(consumerPlugin)
        );
        return Rapunzel.acquire(consumerPlugin, view);
    }

    // ── Context creation (shared) ────────────────────────────────────────────

    private static RapunzelContext createContext(JavaPlugin plugin) {
        Logger logger = plugin.getSLF4JLogger();
        Path dataDir = plugin.getDataFolder().toPath();
        ResourceProvider resources = path -> Optional.ofNullable(openResource(plugin, path));
        MinecraftServer server = PaperHandleBridge.server(plugin);
        Scheduler scheduler = new PaperScheduler(server);
        PlatformRuntime runtime = BootstrapServices.serverRuntime(
            PlatformId.PAPER,
            EngineFamily.MOJANG_SERVER,
            plugin,
            RuntimeProfiles.SERVER_STANDARD
        );

        return SharedBackendBootstrap.createContext(
                plugin,
                plugin.getName(),
                plugin.getName(),
                runtime,
                logger,
                dataDir,
                resources,
                scheduler,
                ctx -> {
                    ctx.register(MinecraftServer.class, server);
                    ctx.register(ConsoleCommandDispatcher.class, new PaperConsoleCommandDispatcher());
                    PlatformFeatures.install(ctx);
                },
                new BackendTransportBootstrap.Hooks(
                    NetworkQueueTransportDecorator.pluginHooks(
                        () -> new PaperPluginMessenger(PaperPlatformBootstrapHost.getPlugin()))
                ),
                PaperPluginMessenger.class,
                PaperPluginMessenger::setNetworkServerName,
                null,
                true
        );
    }

    // ── Legacy entry point for backward compatibility ────────────────────────
    // Kept so existing consumers don't break during migration. Delegates to
    // acquire() which is the correct path when a platform context already exists.

    /**
     * @deprecated Consumer plugins should use {@link #acquire(JavaPlugin)} instead.
     *             This method still works by acquiring the platform context.
     */
    @Deprecated
    public static RapunzelContext bootstrap(JavaPlugin plugin) {
        return acquire(plugin).context();
    }

    @Deprecated
    public static BootstrapHandle bootstrapHandle(JavaPlugin plugin) {
        return acquire(plugin);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static InputStream openResource(JavaPlugin plugin, String path) {
        if (path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return plugin.getResource(normalized);
    }
}
