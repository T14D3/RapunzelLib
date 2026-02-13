package de.t14d3.rapunzellib.platform.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.bootstrap.BootstrapHandle;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
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
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeProfiles;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.minecraft.server.MinecraftServer;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public final class PaperRapunzelBootstrap {
    private PaperRapunzelBootstrap() {
    }

    public static RapunzelContext bootstrap(JavaPlugin plugin) {
        return bootstrapHandle(plugin).context();
    }

    public static BootstrapHandle bootstrapHandle(JavaPlugin plugin) {
        PaperPlatformLoader.ensureLoaded(plugin);
        return Rapunzel.bootstrap(plugin, () -> createContext(plugin));
    }

    public static RapunzelContext createContext(JavaPlugin plugin) {
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
                    PlatformFeatures.install(ctx);
                },
                new BackendTransportBootstrap.Hooks(
                    NetworkQueueTransportDecorator.pluginHooks(() -> new PaperPluginMessenger(plugin))
                ),
                PaperPluginMessenger.class,
                PaperPluginMessenger::setNetworkServerName,
                null,
                true
        );
    }

    private static InputStream openResource(JavaPlugin plugin, String path) {
        if (path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return plugin.getResource(normalized);
    }
}
