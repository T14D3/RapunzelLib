package de.t14d3.rapunzellib.platform.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.bootstrap.BootstrapHandle;
import de.t14d3.rapunzellib.commands.ConsoleCommandDispatcher;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.common.context.ConsumerView;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.network.InMemoryMessenger;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.bootstrap.BackendNetworkInfoBootstrap;
import de.t14d3.rapunzellib.network.bootstrap.BackendTransportBootstrap;
import de.t14d3.rapunzellib.network.bootstrap.SharedBackendBootstrap;
import de.t14d3.rapunzellib.network.info.NetworkInfoClient;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.queue.NetworkQueueTransportDecorator;
import de.t14d3.rapunzellib.platform.PlatformFeatures;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongePersistentAttachmentsStore;
import de.t14d3.rapunzellib.platform.sponge.scheduler.SpongeScheduler;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeProfiles;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Server;
import org.spongepowered.plugin.PluginContainer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public final class SpongeRapunzelBootstrap {
    private SpongeRapunzelBootstrap() {
    }

    // ── Platform bootstrap (called by SpongePlatformPlugin) ─────────────────

    public static BootstrapHandle bootstrapPlatform(PluginContainer container, Path dataDirectory, Server server) {
        SpongePlatformBootstrapHost.prepareBootstrap(container);
        return Rapunzel.bootstrap(container.instance(), () -> createContext(container, dataDirectory, server));
    }

    // ── Consumer acquire ────────────────────────────────────────────────────

    public static BootstrapHandle acquire(PluginContainer container, Path dataDirectory, Server server) {
        RapunzelContext platform = Rapunzel.context();
        Object plugin = container.instance();
        String pluginId = container.metadata().id();
        Logger logger = LoggerFactory.getLogger(pluginId);
        Class<?> resourceAnchor = plugin.getClass();
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
            new LifecycleOwner(container)
        );
        return Rapunzel.acquire(plugin, view);
    }

    // ── Context creation ────────────────────────────────────────────────────

    public static RapunzelContext createContext(
            PluginContainer container,
            Path dataDirectory,
            Server server
    ) {
        if (container == null) throw new IllegalArgumentException("container cannot be null");

        Object plugin = container.instance();
        String pluginId = container.metadata().id();
        Logger logger = LoggerFactory.getLogger(pluginId);
        Class<?> resourceAnchor = plugin.getClass();

        try {
            Files.createDirectories(dataDirectory);
        } catch (Exception e) {
            logger.debug("Failed to create Sponge data directory {}", dataDirectory, e);
        }

        ResourceProvider resources = path -> Optional.ofNullable(openResource(resourceAnchor, path));
        Scheduler scheduler = new SpongeScheduler(server, container);
        PlatformRuntime runtime = BootstrapServices.serverRuntime(
            PlatformId.SPONGE,
            EngineFamily.SPONGE_SERVER,
            container,
            RuntimeProfiles.SERVER_STANDARD
        );

        return SharedBackendBootstrap.createContext(
                plugin,
                pluginId,
                pluginId,
                runtime,
                logger,
                dataDirectory,
                resources,
                scheduler,
                ctx -> {
                    ctx.register(Server.class, server);
                    ctx.register(ConsoleCommandDispatcher.class, new SpongeConsoleCommandDispatcher(server));

                    SpongePersistentAttachmentsStore attachmentStore = ctx.sharedRuntime().getOrCreate(
                        SpongePersistentAttachmentsStore.class,
                        () -> new SpongePersistentAttachmentsStore(
                            logger,
                            ctx.configs(),
                            dataDirectory.resolve("attachments.yml")
                        )
                    );
                    SpongeAttachmentService attachmentService = ctx.sharedRuntime().getOrCreate(
                        SpongeAttachmentService.class,
                        () -> new SpongeAttachmentService(attachmentStore)
                    );
                    ctx.services().register(SpongePersistentAttachmentsStore.class, attachmentStore);
                    ctx.services().register(SpongeAttachmentService.class, attachmentService);

                    PlatformFeatures.install(ctx);
                },
                new BackendTransportBootstrap.Hooks(
                    NetworkQueueTransportDecorator.pluginHooks(() -> new InMemoryMessenger(pluginId, "velocity"))
                ),
                Messenger.class,
                null,
                (ctx, hookScheduler, hookLogger, transport, pluginMessenger, effectiveMessenger) -> {
                    if (transport.pluginMessenger() == null) {
                        return;
                    }
                    NetworkInfoClient networkInfo = BackendNetworkInfoBootstrap.registerClient(
                        ctx,
                        ctx.services().get(NetworkRuntimeGateway.class),
                        hookScheduler,
                        hookLogger
                    );

                    BackendNetworkInfoBootstrap.registerRepeatingTask(ctx, hookScheduler, Duration.ofSeconds(1), Duration.ofSeconds(5), () -> {
                        networkInfo.networkServerName()
                                .thenAccept(name -> hookLogger.debug("Resolved network server name: {}", name))
                                .exceptionally(ignored -> null);
                        return true;
                    });
                },
                false
        );
    }

    // ── Legacy API (deprecated) ─────────────────────────────────────────────

    @Deprecated
    public static RapunzelContext bootstrap(
            PluginContainer container,
            Path dataDirectory,
            Server server
    ) {
        return acquire(container, dataDirectory, server).context();
    }

    @Deprecated
    public static BootstrapHandle bootstrapHandle(
            PluginContainer container,
            Path dataDirectory,
            Server server
    ) {
        return acquire(container, dataDirectory, server);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static InputStream openResource(Class<?> anchor, String path) {
        if (anchor == null || path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader cl = anchor.getClassLoader();
        return (cl != null) ? cl.getResourceAsStream(normalized) : null;
    }
}
