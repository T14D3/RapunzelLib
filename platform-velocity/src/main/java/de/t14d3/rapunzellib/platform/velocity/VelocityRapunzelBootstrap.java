package de.t14d3.rapunzellib.platform.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.RapunzelLibVersion;
import de.t14d3.rapunzellib.bootstrap.BootstrapHandle;
import de.t14d3.rapunzellib.commands.ConsoleCommandDispatcher;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.common.context.ConsumerView;
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
import de.t14d3.rapunzellib.network.runtime.NetworkLinkKind;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntime;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeClassifier;
import de.t14d3.rapunzellib.network.rpcserver.RpcServerConfig;
import de.t14d3.rapunzellib.network.rpcserver.RpcServerMessenger;
import de.t14d3.rapunzellib.network.rpcserver.RoutingHooks;
import de.t14d3.rapunzellib.platform.PlatformFeatures;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityNetworkInfoResponder;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityNetworkInfoService;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityPluginMessenger;
import de.t14d3.rapunzellib.platform.velocity.objects.VelocityPersistentAttachmentsStore;
import de.t14d3.rapunzellib.platform.velocity.scheduler.VelocityScheduler;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeProfiles;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class VelocityRapunzelBootstrap {
    private VelocityRapunzelBootstrap() {
    }

    // ── Platform bootstrap (called by VelocityPlatformPlugin) ───────────────

    public static BootstrapHandle bootstrapPlatform(Object plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        VelocityPlatformBootstrapHost.prepareBootstrap(plugin);
        return Rapunzel.bootstrap(plugin, () -> createContext(plugin, proxy, logger, dataDirectory));
    }

    // ── Consumer acquire ────────────────────────────────────────────────────

    public static BootstrapHandle acquire(Object plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        RapunzelContext platform = Rapunzel.context();
        ConsumerView view = new ConsumerView(
            platform,
            logger,
            dataDirectory,
            path -> Optional.ofNullable(openResource(plugin, path)),
            new LifecycleOwner(plugin)
        );
        return Rapunzel.acquire(plugin, view);
    }

    // ── Context creation ────────────────────────────────────────────────────

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
        ctx.register(ConsoleCommandDispatcher.class, new VelocityConsoleCommandDispatcher(proxy));

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
                // The TCP bridge is created after the transport bootstrap (it needs
                // the plugin messenger for its external-forward hook); a holder lets
                // the queue's canSendToServer override consult it.
                AtomicReference<RpcServerMessenger> tcpBridgeRef = new AtomicReference<>();
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
                                if (proxy.getAllPlayers().stream().anyMatch(player -> player.getCurrentServer()
                                    .map(server -> server.getServerInfo().getName().equalsIgnoreCase(target))
                                    .orElse(false))) {
                                    return true;
                                }
                                // No plugin-message carrier on the target backend, but it
                                // may be reachable over the companion TCP bridge.
                                RpcServerMessenger bridge = tcpBridgeRef.get();
                                return bridge != null && bridge.isServerConnected(target);
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
                    RpcServerMessenger bridge = startPluginTransportTcpBridge(transportConfig, pluginMessenger, proxy, logger, ctx);
                    if (bridge != null) {
                        tcpBridgeRef.set(bridge);
                    }
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

            // The proxy must answer NetworkInfo RPCs (WHO_AM_I, list_servers, list_players)
            // that backends send over the plugin-messaging channel; without a responder the
            // backend can never resolve its network server name and cross-server addressing
            // stays broken. The gateway registered by the transport applier already covers
            // this when plugin messaging is the canonical transport. When a different
            // transport is canonical (Redis/RPC), also answer plugin-channel RPCs via a
            // dedicated plugin-messaging gateway so plugin-transport backends still resolve.
            NetworkRuntimeGateway responderGateway = ctx.services().get(NetworkRuntimeGateway.class);
            if (responderGateway.runtime().canonicalLink().kind() != NetworkLinkKind.PLUGIN_MESSAGING) {
                ctx.services().find(VelocityPluginMessenger.class).ifPresent(pluginMessenger -> {
                    VelocityNetworkInfoResponder pluginResponder = new VelocityNetworkInfoResponder(
                        DefaultNetworkRuntimeGateway.compatibility(pluginMessenger),
                        proxy,
                        logger
                    );
                    ctx.register(VelocityNetworkInfoResponder.class, pluginResponder);
                    ctx.registerCloseable(pluginResponder);
                });
            }

            VelocityNetworkInfoResponder networkInfoResponder = new VelocityNetworkInfoResponder(
                responderGateway,
                proxy,
                logger
            );
            ctx.register(VelocityNetworkInfoResponder.class, networkInfoResponder);
            ctx.registerCloseable(networkInfoResponder);

            VelocityNetworkInfoService networkInfo = new VelocityNetworkInfoService(
                ctx.services().get(NetworkRuntime.class),
                proxy
            );
            ctx.registerLinked(VelocityNetworkInfoService.class, networkInfo, NetworkInfoService.class);

        return ctx;
    }

    // ── Legacy API (deprecated) ─────────────────────────────────────────────

    @Deprecated
    public static RapunzelContext bootstrap(Object plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        return acquire(plugin, proxy, logger, dataDirectory).context();
    }

    @Deprecated
    public static BootstrapHandle bootstrapHandle(Object plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        return acquire(plugin, proxy, logger, dataDirectory);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Starts the companion TCP bridge (RPC server) for the plugin transport.
     *
     * <p>Plugin messaging can only reach the proxy through a player connection
     * that traverses it. On networks where players connect directly to backends
     * (or no carrier is available) the plugin channel alone cannot carry
     * envelopes. This bridge lets backends connect to the proxy over plain TCP
     * and exchange the same envelopes, while the plugin channel remains the
     * carrier whenever a player connection is available.</p>
     *
     * @return the started bridge, or null if it could not be started
     */
    private static RpcServerMessenger startPluginTransportTcpBridge(
        de.t14d3.rapunzellib.config.YamlConfig transportConfig,
        VelocityPluginMessenger pluginMessenger,
        ProxyServer proxy,
        Logger logger,
        RapunzelContext ctx
    ) {
        try {
            String serverName = firstNonBlank(
                MessengerTransportBootstrap.resolveNames(transportConfig, PlatformId.VELOCITY).serverName(),
                NetworkDefaults.DEFAULT_PROXY_SERVER_NAME
            );
            String bindHost = firstNonBlank(
                transportConfig.getString("network.rpcServer.host", null),
                NetworkDefaults.DEFAULT_RPC_BIND_HOST
            );
            long port = transportConfig.getLong("network.rpcServer.port", NetworkDefaults.DEFAULT_RPC_PORT);
            int portInt = (port >= 1 && port <= 65535) ? (int) port : NetworkDefaults.DEFAULT_RPC_PORT;

            RpcServerConfig config = RpcServerConfig.builder(serverName)
                .bindHost(bindHost)
                .port(portInt)
                .build();
            RoutingHooks routingHooks = new RoutingHooks(
                () -> proxy.getAllServers().stream()
                    .map(rs -> rs.getServerInfo().getName())
                    .toList(),
                (channel, data, sourceServer, targetServer) ->
                    pluginMessenger.forwardViaPluginChannel(targetServer, channel, data, sourceServer)
            );
            RpcServerMessenger bridge = new RpcServerMessenger(config, logger, routingHooks);
            pluginMessenger.attachTcpBridge(bridge);
            ctx.registerCloseable(bridge);
            logger.info("[Network] Plugin transport TCP bridge listening on {}:{}", bindHost, portInt);
            return bridge;
        } catch (Exception e) {
            logger.warn("[Network] Failed to start plugin transport TCP bridge; "
                + "plugin-messaging carriers will be used exclusively", e);
            return null;
        }
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
