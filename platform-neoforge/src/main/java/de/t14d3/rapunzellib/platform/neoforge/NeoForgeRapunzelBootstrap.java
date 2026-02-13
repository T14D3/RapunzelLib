package de.t14d3.rapunzellib.platform.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.bootstrap.BootstrapHandle;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.network.bootstrap.BackendTransportBootstrap;
import de.t14d3.rapunzellib.network.bootstrap.SharedBackendBootstrap;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.network.queue.NetworkQueueTransportDecorator;
import de.t14d3.rapunzellib.platform.PlatformFeatures;
import de.t14d3.rapunzellib.platform.neoforge.network.NeoForgePluginMessenger;
import de.t14d3.rapunzellib.platform.neoforge.scheduler.NeoForgeScheduler;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeProfiles;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class NeoForgeRapunzelBootstrap {
    public static final String MOD_ID = "rapunzellib_platform_neoforge";

    private NeoForgeRapunzelBootstrap() {
    }

    public static RapunzelContext bootstrap(
            String modId,
            MinecraftServer server,
            Logger logger,
            Path dataDirectory,
            Class<?> resourceAnchor
    ) {
        return bootstrapHandle(modId, server, logger, dataDirectory, resourceAnchor).context();
    }

    public static BootstrapHandle bootstrapHandle(
            String modId,
            MinecraftServer server,
            Logger logger,
            Path dataDirectory,
            Class<?> resourceAnchor
    ) {
        return Rapunzel.bootstrap(modId, () -> createContext(modId, server, logger, dataDirectory, resourceAnchor));
    }

    public static RapunzelContext createContext(
            String modId,
            MinecraftServer server,
            Logger logger,
            Path dataDirectory,
            Class<?> resourceAnchor
    ) {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(dataDirectory, "dataDirectory");

        try {
            Files.createDirectories(dataDirectory);
        } catch (Exception e) {
            logger.debug("Failed to create NeoForge data directory {}", dataDirectory, e);
        }

        ResourceProvider resources = path -> Optional.ofNullable(openResource(resourceAnchor, path));
        Scheduler scheduler = new NeoForgeScheduler(server);
        PlatformRuntime runtime = BootstrapServices.serverRuntime(
            PlatformId.NEOFORGE,
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
                dataDirectory,
                resources,
                scheduler,
                ctx -> {
                    ctx.register(MinecraftServer.class, server);
                    PlatformFeatures.install(ctx);
                },
                new BackendTransportBootstrap.Hooks(
                    NetworkQueueTransportDecorator.pluginHooks(() -> new NeoForgePluginMessenger(server, logger))
                ),
                NeoForgePluginMessenger.class,
                NeoForgePluginMessenger::setNetworkServerName
        );
    }

    private static InputStream openResource(Class<?> anchor, String path) {
        if (anchor == null || path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader cl = anchor.getClassLoader();
        return (cl != null) ? cl.getResourceAsStream(normalized) : null;
    }
}
