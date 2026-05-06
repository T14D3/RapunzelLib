package de.t14d3.rapunzellib.platform.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.RapunzelLibVersion;
import de.t14d3.rapunzellib.bootstrap.BootstrapHandle;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.network.InMemoryMessenger;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.bootstrap.BackendTransportBootstrap;
import de.t14d3.rapunzellib.network.bootstrap.MessengerTransportBootstrap;
import de.t14d3.rapunzellib.network.bootstrap.TransportBootstrapResultApplier;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.NetworkDefaults;
import de.t14d3.rapunzellib.network.queue.NetworkQueueTransportDecorator;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntime;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeClassifier;
import de.t14d3.rapunzellib.platform.PlatformFeatures;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityNetworkInfoResponder;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityNetworkInfoService;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityPluginMessenger;
import de.t14d3.rapunzellib.platform.velocity.objects.VelocityPersistentAttachmentsStore;
import de.t14d3.rapunzellib.platform.velocity.scheduler.VelocityScheduler;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeProfiles;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public final class VelocityRapunzelBootstrap {
    private VelocityRapunzelBootstrap() {
    }

    public static RapunzelContext bootstrap(Object plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        return bootstrapHandle(plugin, proxy, logger, dataDirectory).context();
    }

    public static BootstrapHandle bootstrapHandle(Object plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        VelocityPlatformBootstrapHost.prepareBootstrap(plugin);
        return Rapunzel.bootstrap(plugin, () -> createContext(plugin, proxy, logger, dataDirectory));
    }

    public static RapunzelContext createContext(Object plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        logger.info("Bootstrapping RapunzelLib {}", RapunzelLibVersion.current());

        ResourceProvider resources = path -> Optional.ofNullable(openResource(plugin, path));
        Scheduler scheduler = new VelocityScheduler(proxy, plugin);
        PlatformRuntime runtime = BootstrapServices.proxyRuntime(
            PlatformId.VELOCITY,
            plugin,
            RuntimeProfiles.PROXY_STANDARD
        );

        BootstrapServices.FirstPhaseResult firstPhase =
            BootstrapServices.bootstrapFirstPhase(runtime, logger, dataDirectory, resources, scheduler);
        RapunzelContext ctx = firstPhase.context();
        ctx.register(ProxyServer.class, proxy);

        ConfigService configService = firstPhase.configService();

            VelocityPersistentAttachmentsStore persistentAttachmentsStore = ctx.sharedRuntime().getOrCreate(
                VelocityPersistentAttachmentsStore.class,
                () -> new VelocityPersistentAttachmentsStore(
                    logger,
                    configService,
                    dataDirectory.resolve("attachments.yml")
                )
            );
            ctx.services().register(VelocityPersistentAttachmentsStore.class, persistentAttachmentsStore);
            PlatformFeatures.install(ctx);

            var transportConfig = configService.load(dataDirectory.resolve("config.yml"), "config.yml");
            MessengerTransportBootstrap.ResolvedNames resolvedNames = MessengerTransportBootstrap.resolveNames(
                transportConfig,
                PlatformId.VELOCITY
            );
            InMemoryMessenger inMemory = ctx.sharedRuntime().getOrCreate(
                InMemoryMessenger.class,
                () -> new InMemoryMessenger(
                    firstNonBlank(resolvedNames.serverName(), NetworkDefaults.DEFAULT_PROXY_SERVER_NAME),
                    firstNonBlank(resolvedNames.proxyServerName(), NetworkDefaults.DEFAULT_PROXY_SERVER_NAME)
                )
            );
            ctx.register(Messenger.class, inMemory);
            ctx.register(InMemoryMessenger.class, inMemory);

            Messenger messenger = inMemory;
            try {
                String ownerId = runtime.persistentOwnerId(dataDirectory);
                BackendTransportBootstrap.Result transport = BackendTransportBootstrap.bootstrap(
                    transportConfig,
                    PlatformId.VELOCITY,
                    logger,
                    ctx.services(),
                    scheduler,
                    inMemory,
                    ownerId,
                    new BackendTransportBootstrap.Hooks(
                        NetworkQueueTransportDecorator.pluginHooks(
                            () -> new VelocityPluginMessenger(plugin, proxy, logger),
                            () -> proxy.getAllServers().stream()
                                .map(rs -> rs.getServerInfo().getName())
                                .toList(),
                            targetServer -> {
                                if (targetServer == null || targetServer.isBlank()) {
                                    return false;
                                }
                                String target = targetServer.trim();
                                return proxy.getAllPlayers().stream()
                                    .anyMatch(player -> player.getCurrentServer()
                                        .map(server -> server.getServerInfo().getName().equalsIgnoreCase(target))
                                        .orElse(false));
                            },
                            null,
                            (pluginTransport, pluginEffective) -> {
                                if (pluginTransport instanceof VelocityPluginMessenger velocityPlugin
                                    && pluginEffective != velocityPlugin) {
                                    velocityPlugin.setUndeliverableForwarder(pluginEffective);
                                }
                            }
                        )
                    )
                );

                if (transport.pluginMessenger() instanceof VelocityPluginMessenger pluginMessenger) {
                    ctx.register(VelocityPluginMessenger.class, pluginMessenger);
                }

                messenger = TransportBootstrapResultApplier.apply(ctx, logger, transport);
            } catch (Exception e) {
                logger.warn("Failed to initialize network transport; using in-memory.", e);
                ctx.register(Messenger.class, inMemory);
                NetworkRuntime fallbackRuntime = NetworkRuntimeClassifier.fallback(PlatformId.VELOCITY, inMemory);
                ctx.register(NetworkRuntime.class, fallbackRuntime);
                ctx.registerLinked(
                    DefaultNetworkRuntimeGateway.class,
                    new DefaultNetworkRuntimeGateway(fallbackRuntime, scheduler, logger),
                    NetworkRuntimeGateway.class
                );
            }

            VelocityNetworkInfoResponder networkInfoResponder = new VelocityNetworkInfoResponder(
                ctx.services().get(NetworkRuntimeGateway.class),
                proxy,
                logger
            );
            ctx.register(VelocityNetworkInfoResponder.class, networkInfoResponder);

            VelocityNetworkInfoService networkInfo = new VelocityNetworkInfoService(
                ctx.services().get(NetworkRuntime.class),
                proxy
            );
            ctx.registerLinked(VelocityNetworkInfoService.class, networkInfo, NetworkInfoService.class);

        return ctx;
    }

    private static InputStream openResource(Object plugin, String path) {
        if (path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader cl = plugin.getClass().getClassLoader();
        return cl.getResourceAsStream(normalized);
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first.trim() : fallback;
    }
}
