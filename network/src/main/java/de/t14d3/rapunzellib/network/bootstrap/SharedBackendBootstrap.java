package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.RapunzelLibVersion;
import de.t14d3.rapunzellib.bootstrap.BootstrapHandle;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.network.InMemoryMessenger;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkDefaults;
import de.t14d3.rapunzellib.network.info.NetworkInfoClient;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntime;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeClassifier;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shared bootstrap logic for backend network initialization.
 */
public final class SharedBackendBootstrap {
    private SharedBackendBootstrap() {
    }

    @FunctionalInterface
    public interface TransportReadyHook<PM extends Messenger> {
        void onReady(
            RapunzelContext context,
            Scheduler scheduler,
            Logger logger,
            BackendTransportBootstrap.Result transport,
            PM pluginMessenger,
            Messenger effectiveMessenger
        );
    }

    public static <PM extends Messenger> RapunzelContext bootstrap(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        Supplier<? extends Messenger> pluginMessengerFactory,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook
    ) {
        return bootstrap(
            leaseOwner,
            leaseName,
            inMemorySourceId,
            runtime,
            logger,
            dataDir,
            resources,
            scheduler,
            worldAccessorRegistration,
            BackendTransportBootstrap.Hooks.standard(pluginMessengerFactory),
            pluginMessengerType,
            pluginNetworkServerNameHook,
            null,
            true
        );
    }

    public static <PM extends Messenger> RapunzelContext bootstrap(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        Supplier<? extends Messenger> pluginMessengerFactory,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook,
        TransportReadyHook<PM> transportReadyHook
    ) {
        return bootstrap(
            leaseOwner,
            leaseName,
            inMemorySourceId,
            runtime,
            logger,
            dataDir,
            resources,
            scheduler,
            worldAccessorRegistration,
            BackendTransportBootstrap.Hooks.standard(pluginMessengerFactory),
            pluginMessengerType,
            pluginNetworkServerNameHook,
            transportReadyHook,
            true
        );
    }

    public static <PM extends Messenger> RapunzelContext bootstrap(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        BackendTransportBootstrap.Hooks transportHooks,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook
    ) {
        return bootstrap(
            leaseOwner,
            leaseName,
            inMemorySourceId,
            runtime,
            logger,
            dataDir,
            resources,
            scheduler,
            worldAccessorRegistration,
            transportHooks,
            pluginMessengerType,
            pluginNetworkServerNameHook,
            null,
            true
        );
    }

    public static <PM extends Messenger> RapunzelContext bootstrap(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        BackendTransportBootstrap.Hooks transportHooks,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook,
        TransportReadyHook<PM> transportReadyHook
    ) {
        return bootstrap(
            leaseOwner,
            leaseName,
            inMemorySourceId,
            runtime,
            logger,
            dataDir,
            resources,
            scheduler,
            worldAccessorRegistration,
            transportHooks,
            pluginMessengerType,
            pluginNetworkServerNameHook,
            transportReadyHook,
            true
        );
    }

    public static <PM extends Messenger> RapunzelContext bootstrap(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        Supplier<? extends Messenger> pluginMessengerFactory,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook,
        TransportReadyHook<PM> transportReadyHook,
        boolean registerDefaultNetworkInfo
    ) {
        return bootstrap(
            leaseOwner,
            leaseName,
            inMemorySourceId,
            runtime,
            logger,
            dataDir,
            resources,
            scheduler,
            worldAccessorRegistration,
            BackendTransportBootstrap.Hooks.standard(pluginMessengerFactory),
            pluginMessengerType,
            pluginNetworkServerNameHook,
            transportReadyHook,
            registerDefaultNetworkInfo
        );
    }

    public static <PM extends Messenger> RapunzelContext bootstrap(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        BackendTransportBootstrap.Hooks transportHooks,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook,
        TransportReadyHook<PM> transportReadyHook,
        boolean registerDefaultNetworkInfo
    ) {
        return bootstrapHandle(
            leaseOwner,
            leaseName,
            inMemorySourceId,
            runtime,
            logger,
            dataDir,
            resources,
            scheduler,
            worldAccessorRegistration,
            transportHooks,
            pluginMessengerType,
            pluginNetworkServerNameHook,
            transportReadyHook,
            registerDefaultNetworkInfo
        ).context();
    }

    public static <PM extends Messenger> BootstrapHandle bootstrapHandle(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        BackendTransportBootstrap.Hooks transportHooks,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook,
        TransportReadyHook<PM> transportReadyHook,
        boolean registerDefaultNetworkInfo
    ) {
        return Rapunzel.bootstrap(leaseOwner, () -> createContext(
            leaseOwner,
            leaseName,
            inMemorySourceId,
            runtime,
            logger,
            dataDir,
            resources,
            scheduler,
            worldAccessorRegistration,
            transportHooks,
            pluginMessengerType,
            pluginNetworkServerNameHook,
            transportReadyHook,
            registerDefaultNetworkInfo
        ));
    }

    public static <PM extends Messenger> RapunzelContext createContext(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        Supplier<? extends Messenger> pluginMessengerFactory,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook
    ) {
        return createContext(
            leaseOwner,
            leaseName,
            inMemorySourceId,
            runtime,
            logger,
            dataDir,
            resources,
            scheduler,
            worldAccessorRegistration,
            BackendTransportBootstrap.Hooks.standard(pluginMessengerFactory),
            pluginMessengerType,
            pluginNetworkServerNameHook,
            null,
            true
        );
    }

    public static <PM extends Messenger> RapunzelContext createContext(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        Supplier<? extends Messenger> pluginMessengerFactory,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook,
        TransportReadyHook<PM> transportReadyHook
    ) {
        return createContext(
            leaseOwner,
            leaseName,
            inMemorySourceId,
            runtime,
            logger,
            dataDir,
            resources,
            scheduler,
            worldAccessorRegistration,
            BackendTransportBootstrap.Hooks.standard(pluginMessengerFactory),
            pluginMessengerType,
            pluginNetworkServerNameHook,
            transportReadyHook,
            true
        );
    }

    public static <PM extends Messenger> RapunzelContext createContext(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        BackendTransportBootstrap.Hooks transportHooks,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook
    ) {
        return createContext(
            leaseOwner,
            leaseName,
            inMemorySourceId,
            runtime,
            logger,
            dataDir,
            resources,
            scheduler,
            worldAccessorRegistration,
            transportHooks,
            pluginMessengerType,
            pluginNetworkServerNameHook,
            null,
            true
        );
    }

    public static <PM extends Messenger> RapunzelContext createContext(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        BackendTransportBootstrap.Hooks transportHooks,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook,
        TransportReadyHook<PM> transportReadyHook
    ) {
        return createContext(
            leaseOwner,
            leaseName,
            inMemorySourceId,
            runtime,
            logger,
            dataDir,
            resources,
            scheduler,
            worldAccessorRegistration,
            transportHooks,
            pluginMessengerType,
            pluginNetworkServerNameHook,
            transportReadyHook,
            true
        );
    }

    public static <PM extends Messenger> RapunzelContext createContext(
        Object leaseOwner,
        String leaseName,
        String inMemorySourceId,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler,
        Consumer<RapunzelContext> worldAccessorRegistration,
        BackendTransportBootstrap.Hooks transportHooks,
        Class<PM> pluginMessengerType,
        BiConsumer<PM, String> pluginNetworkServerNameHook,
        TransportReadyHook<PM> transportReadyHook,
        boolean registerDefaultNetworkInfo
    ) {
        Objects.requireNonNull(leaseOwner, "leaseOwner");
        Objects.requireNonNull(leaseName, "leaseName");
        Objects.requireNonNull(inMemorySourceId, "inMemorySourceId");
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(dataDir, "dataDir");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(worldAccessorRegistration, "worldAccessorRegistration");
        Objects.requireNonNull(pluginMessengerType, "pluginMessengerType");

        logger.info("Bootstrapping RapunzelLib {}", RapunzelLibVersion.current());

        BootstrapServices.FirstPhaseResult firstPhase =
            BootstrapServices.bootstrapFirstPhase(runtime, logger, dataDir, resources, scheduler);
        RapunzelContext context = firstPhase.context();

        worldAccessorRegistration.accept(context);

        ConfigService configService = firstPhase.configService();

        try {
            var transportConfig = configService.load(dataDir.resolve("config.yml"), "config.yml");
            MessengerTransportBootstrap.ResolvedNames resolvedNames = MessengerTransportBootstrap.resolveNames(
                transportConfig,
                runtime.platformId()
            );
            InMemoryMessenger inMemoryMessenger = context.sharedRuntime().getOrCreate(
                InMemoryMessenger.class,
                () -> new InMemoryMessenger(
                    firstNonBlank(resolvedNames.serverName(), inMemorySourceId),
                    firstNonBlank(resolvedNames.proxyServerName(), NetworkDefaults.DEFAULT_PROXY_SERVER_NAME)
                )
            );
            context.register(Messenger.class, inMemoryMessenger);
            context.register(InMemoryMessenger.class, inMemoryMessenger);

            String ownerId = runtime.persistentOwnerId(dataDir);
            BackendTransportBootstrap.Result transport = BackendTransportBootstrap.bootstrap(
                transportConfig,
                runtime.platformId(),
                logger,
                context.services(),
                scheduler,
                inMemoryMessenger,
                ownerId,
                transportHooks
            );

            PM pluginMessenger = pluginMessengerType.isInstance(transport.pluginMessenger())
                ? pluginMessengerType.cast(transport.pluginMessenger())
                : null;
            if (pluginMessenger != null) {
                context.register(pluginMessengerType, pluginMessenger);
            }

            Messenger effectiveMessenger = TransportBootstrapResultApplier.apply(context, logger, transport);

            if (transportReadyHook != null) {
                transportReadyHook.onReady(context, scheduler, logger, transport, pluginMessenger, effectiveMessenger);
            }

            if (registerDefaultNetworkInfo && !(effectiveMessenger instanceof InMemoryMessenger)) {
                if (pluginMessenger != null && pluginNetworkServerNameHook != null) {
                    BackendNetworkInfoBootstrap.registerClientAndBindServerName(
                        context,
                        context.services().get(NetworkRuntimeGateway.class),
                        scheduler,
                        logger,
                        pluginMessenger,
                        pluginNetworkServerNameHook
                    );
                } else {
                    BackendNetworkInfoBootstrap.registerClient(
                        context,
                        context.services().get(NetworkRuntimeGateway.class),
                        scheduler,
                        logger
                    );
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to initialize network transport; using in-memory.", e);
            InMemoryMessenger inMemoryMessenger = context.sharedRuntime().getOrCreate(
                InMemoryMessenger.class,
                () -> new InMemoryMessenger(inMemorySourceId, NetworkDefaults.DEFAULT_PROXY_SERVER_NAME)
            );
            context.services().register(Messenger.class, inMemoryMessenger);
            context.services().register(InMemoryMessenger.class, inMemoryMessenger);
            NetworkRuntime fallbackRuntime = NetworkRuntimeClassifier.fallback(runtime.platformId(), inMemoryMessenger);
            context.register(NetworkRuntime.class, fallbackRuntime);
            context.registerLinked(
                DefaultNetworkRuntimeGateway.class,
                new DefaultNetworkRuntimeGateway(fallbackRuntime, scheduler, logger),
                NetworkRuntimeGateway.class
            );
        }

        return context;
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first.trim() : fallback;
    }
}
