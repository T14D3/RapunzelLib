package de.t14d3.rapunzellib.platform.fabric;

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
import de.t14d3.rapunzellib.platform.PlatformFeatures;
import de.t14d3.rapunzellib.platform.fabric.network.FabricPluginMessenger;
import de.t14d3.rapunzellib.platform.fabric.scheduler.FabricScheduler;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeProfiles;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class FabricRapunzelBootstrap {
    public static final String PLATFORM_MOD_ID = FabricPlatformBootstrapHost.MOD_ID;

    private FabricRapunzelBootstrap() {
    }

    // ── Platform bootstrap (called by FabricPlatformMod lifecycle hooks) ─────

    public static BootstrapHandle bootstrapPlatform(MinecraftServer server) {
        return Rapunzel.bootstrap(PLATFORM_MOD_ID, () -> createContext(PLATFORM_MOD_ID, server, FabricPlatformMod.class));
    }

    // ── Consumer acquire ────────────────────────────────────────────────────

    public static BootstrapHandle acquire(String modId, MinecraftServer server, Class<?> resourceAnchor) {
        RapunzelContext platform = Rapunzel.context();
        Logger logger = LoggerFactory.getLogger(modId);
        Path dataDir = FabricLoader.getInstance().getConfigDir().resolve(modId);
        try {
            Files.createDirectories(dataDir);
        } catch (Exception e) {
            logger.debug("Failed to create data directory {}", dataDir, e);
        }
        ConsumerView view = new ConsumerView(
            platform,
            logger,
            dataDir,
            path -> Optional.ofNullable(openResource(resourceAnchor, path)),
            new LifecycleOwner(modId)
        );
        return Rapunzel.acquire(modId, view);
    }

    // ── Context creation ────────────────────────────────────────────────────

    private static RapunzelContext createContext(String modId, MinecraftServer server, Class<?> resourceAnchor) {
        Logger logger = LoggerFactory.getLogger(modId);
        Path dataDir = FabricLoader.getInstance().getConfigDir().resolve(modId);
        try {
            Files.createDirectories(dataDir);
        } catch (Exception e) {
            logger.debug("Failed to create Fabric config directory {}", dataDir, e);
        }
        ResourceProvider resources = path -> Optional.ofNullable(openResource(resourceAnchor, path));
        Scheduler scheduler = new FabricScheduler(server);
        PlatformRuntime runtime = BootstrapServices.serverRuntime(
            PlatformId.FABRIC,
            EngineFamily.MOJANG_SERVER,
            server,
            RuntimeProfiles.SERVER_STANDARD
        );
        return SharedBackendBootstrap.createContext(
                modId,
                modId,
                modId,
                runtime,
                logger,
                dataDir,
                resources,
                scheduler,
                ctx -> {
                    ctx.register(MinecraftServer.class, server);
                    ctx.register(ConsoleCommandDispatcher.class, new FabricConsoleCommandDispatcher(server));
                    PlatformFeatures.install(ctx);
                },
                new BackendTransportBootstrap.Hooks(
                    NetworkQueueTransportDecorator.pluginHooks(() -> new FabricPluginMessenger(server, logger))
                ),
                FabricPluginMessenger.class,
                FabricPluginMessenger::setNetworkServerName
        );
    }

    // ── Legacy API (deprecated) ─────────────────────────────────────────────

    @Deprecated
    public static RapunzelContext bootstrap(String modId, MinecraftServer server, Class<?> resourceAnchor) {
        return acquire(modId, server, resourceAnchor).context();
    }

    @Deprecated
    public static BootstrapHandle bootstrapHandle(String modId, MinecraftServer server, Class<?> resourceAnchor) {
        return acquire(modId, server, resourceAnchor);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static InputStream openResource(Class<?> anchor, String path) {
        if (anchor == null || path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader cl = anchor.getClassLoader();
        return (cl != null) ? cl.getResourceAsStream(normalized) : null;
    }
}
