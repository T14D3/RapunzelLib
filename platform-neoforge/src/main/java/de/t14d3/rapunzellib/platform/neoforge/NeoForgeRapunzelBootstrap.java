package de.t14d3.rapunzellib.platform.neoforge;

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
import de.t14d3.rapunzellib.platform.neoforge.network.NeoForgePluginMessenger;
import de.t14d3.rapunzellib.platform.neoforge.scheduler.NeoForgeScheduler;
import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.attachments.SharedPersistentAttachmentsStore;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeProfiles;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class NeoForgeRapunzelBootstrap {
    public static final String MOD_ID = "rapunzellib_platform_neoforge";

    private NeoForgeRapunzelBootstrap() {
    }

    // ── Platform bootstrap (called by NeoForgePlatformMod lifecycle hooks) ──

    public static BootstrapHandle bootstrapPlatform(MinecraftServer server) {
        Logger logger = LoggerFactory.getLogger(MOD_ID);
        Path dataDir = Path.of("").resolve("config").resolve(MOD_ID);
        return Rapunzel.bootstrap(MOD_ID, () -> createContext(MOD_ID, server, logger, dataDir, NeoForgePlatformMod.class));
    }

    // ── Consumer acquire ────────────────────────────────────────────────────

    public static BootstrapHandle acquire(String modId, MinecraftServer server, Logger logger, Path dataDirectory, Class<?> resourceAnchor) {
        RapunzelContext platform = Rapunzel.context();
        try {
            Files.createDirectories(dataDirectory);
        } catch (Exception e) {
            logger.debug("Failed to create data directory {}", dataDirectory, e);
        }
        ConsumerView view = new ConsumerView(
            platform,
            logger,
            dataDirectory,
            path -> Optional.ofNullable(openResource(resourceAnchor, path)),
            new LifecycleOwner(modId)
        );
        return Rapunzel.acquire(modId, view);
    }

    // ── Context creation ────────────────────────────────────────────────────

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
                    ctx.register(ConsoleCommandDispatcher.class, new NeoForgeConsoleCommandDispatcher(server));

                    SharedPersistentAttachmentsStore attachmentStore = ctx.sharedRuntime().getOrCreate(
                        SharedPersistentAttachmentsStore.class,
                        () -> new SharedPersistentAttachmentsStore(
                            logger,
                            ctx.configs(),
                            dataDirectory.resolve("attachments.yml")
                        )
                    );
                    SharedAttachmentService attachmentService = ctx.sharedRuntime().getOrCreate(
                        SharedAttachmentService.class,
                        () -> new SharedAttachmentService(attachmentStore)
                    );
                    ctx.services().register(SharedPersistentAttachmentsStore.class, attachmentStore);
                    ctx.services().register(SharedAttachmentService.class, attachmentService);

                    PlatformFeatures.install(ctx);
                },
                new BackendTransportBootstrap.Hooks(
                    NetworkQueueTransportDecorator.pluginHooks(() -> new NeoForgePluginMessenger(server, logger))
                ),
                NeoForgePluginMessenger.class,
                NeoForgePluginMessenger::setNetworkServerName
        );
    }

    // ── Legacy API (deprecated) ─────────────────────────────────────────────

    @Deprecated
    public static RapunzelContext bootstrap(
            String modId,
            MinecraftServer server,
            Logger logger,
            Path dataDirectory,
            Class<?> resourceAnchor
    ) {
        return acquire(modId, server, logger, dataDirectory, resourceAnchor).context();
    }

    @Deprecated
    public static BootstrapHandle bootstrapHandle(
            String modId,
            MinecraftServer server,
            Logger logger,
            Path dataDirectory,
            Class<?> resourceAnchor
    ) {
        return acquire(modId, server, logger, dataDirectory, resourceAnchor);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static InputStream openResource(Class<?> anchor, String path) {
        if (anchor == null || path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader cl = anchor.getClassLoader();
        return (cl != null) ? cl.getResourceAsStream(normalized) : null;
    }
}
