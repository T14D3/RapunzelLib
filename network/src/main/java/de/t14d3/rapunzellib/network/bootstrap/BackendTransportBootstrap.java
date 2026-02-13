package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.network.InMemoryMessenger;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class BackendTransportBootstrap {
    private BackendTransportBootstrap() {
    }

    @FunctionalInterface
    public interface OptionalTransportBootstrap {
        MessengerTransportBootstrap.Result bootstrap(TransportContext context);
    }

    @FunctionalInterface
    public interface PluginTransportDecorator {
        DecoratedPluginTransport decorate(Messenger pluginMessenger, TransportContext context, PluginHooks pluginHooks);
    }

    public record DecoratedPluginTransport(Messenger messenger, AutoCloseable closeable) {
        public DecoratedPluginTransport {
            Objects.requireNonNull(messenger, "messenger");
        }

        public static DecoratedPluginTransport passthrough(Messenger messenger) {
            return new DecoratedPluginTransport(messenger, null);
        }
    }

    public record PluginHooks(
        Supplier<? extends Messenger> messengerFactory,
        Supplier<List<String>> allServersSupplier,
        Predicate<String> canSendToServerOverride,
        PluginTransportDecorator transportDecorator,
        BiConsumer<Messenger, Messenger> onPluginEffective
    ) {
        public PluginHooks(
            Supplier<? extends Messenger> messengerFactory,
            Supplier<List<String>> allServersSupplier,
            Predicate<String> canSendToServerOverride,
            BiConsumer<Messenger, Messenger> onPluginEffective
        ) {
            this(messengerFactory, allServersSupplier, canSendToServerOverride, (PluginTransportDecorator) null, onPluginEffective);
        }

        public PluginHooks(
            Supplier<? extends Messenger> messengerFactory,
            Supplier<List<String>> allServersSupplier,
            Predicate<String> canSendToServerOverride,
            Object ignoredQueueListener,
            BiConsumer<Messenger, Messenger> onPluginEffective
        ) {
            this(messengerFactory, allServersSupplier, canSendToServerOverride, (PluginTransportDecorator) null, onPluginEffective);
        }

        public static PluginHooks standard(Supplier<? extends Messenger> messengerFactory) {
            return new PluginHooks(messengerFactory, null, null, (PluginTransportDecorator) null, null);
        }

        public PluginHooks withTransportDecorator(PluginTransportDecorator decorator) {
            return new PluginHooks(messengerFactory, allServersSupplier, canSendToServerOverride, decorator, onPluginEffective);
        }
    }

    public record Hooks(
        PluginHooks pluginHooks
    ) {
        public static Hooks standard(Supplier<? extends Messenger> pluginMessengerFactory) {
            return new Hooks(pluginMessengerFactory != null ? PluginHooks.standard(pluginMessengerFactory) : null);
        }
    }

    public record TransportContext(
        YamlConfig transportConfig,
        PlatformId platformId,
        Logger logger,
        ServiceRegistry services,
        Scheduler scheduler,
        InMemoryMessenger inMemoryMessenger,
        String ownerId,
        MessengerTransportBootstrap.TransportPriority priority,
        String serverName,
        String proxyServerName
    ) {
    }

    public record Result(
        PlatformId platformId,
        String serverName,
        String proxyServerName,
        MessengerTransportBootstrap.TransportPriority priority,
        Messenger pluginMessenger,
        Messenger pluginEffective,
        Messenger redisMessenger,
        Messenger rpcMessenger,
        Messenger effectiveMessenger,
        AutoCloseable queueCloseable,
        AutoCloseable redisCloseable,
        AutoCloseable rpcCloseable
    ) {
    }

    public static Result bootstrap(
        YamlConfig transportConfig,
        PlatformId platformId,
        Logger logger,
        ServiceRegistry services,
        Scheduler scheduler,
        InMemoryMessenger inMemoryMessenger,
        String ownerId,
        Supplier<? extends Messenger> pluginMessengerFactory
    ) {
        return bootstrap(
            transportConfig,
            platformId,
            logger,
            services,
            scheduler,
            inMemoryMessenger,
            ownerId,
            Hooks.standard(pluginMessengerFactory)
        );
    }

    public static Result bootstrap(
        YamlConfig transportConfig,
        PlatformId platformId,
        Logger logger,
        ServiceRegistry services,
        Scheduler scheduler,
        InMemoryMessenger inMemoryMessenger,
        String ownerId,
        Hooks hooks
    ) {
        Objects.requireNonNull(transportConfig, "transportConfig");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(inMemoryMessenger, "inMemoryMessenger");
        Objects.requireNonNull(ownerId, "ownerId");

        MessengerTransportBootstrap.TransportPriority priority = MessengerTransportBootstrap.resolvePriority(transportConfig);
        MessengerTransportBootstrap.ResolvedNames names = MessengerTransportBootstrap.resolveNames(transportConfig, platformId);
        TransportContext transportContext = new TransportContext(
            transportConfig,
            platformId,
            logger,
            services,
            scheduler,
            inMemoryMessenger,
            ownerId,
            priority,
            names.serverName(),
            names.proxyServerName()
        );

        Messenger pluginMessenger = null;
        Messenger pluginEffective = null;
        AutoCloseable queueCloseable = null;

        PluginHooks pluginHooks = hooks != null ? hooks.pluginHooks() : null;
        Supplier<? extends Messenger> pluginMessengerFactory = pluginHooks != null ? pluginHooks.messengerFactory() : null;
        if (shouldInitializePluginTransport(priority) && pluginMessengerFactory != null) {
            pluginMessenger = pluginMessengerFactory.get();
            if (pluginMessenger != null) {
                DecoratedPluginTransport decorated = decoratePluginTransport(pluginMessenger, transportContext, pluginHooks);
                pluginEffective = decorated.messenger();
                queueCloseable = decorated.closeable();
                services.register(Messenger.class, pluginEffective);
                if (pluginHooks != null && pluginHooks.onPluginEffective() != null) {
                    pluginHooks.onPluginEffective().accept(pluginMessenger, pluginEffective);
                }
            }
        }

        Messenger redisMessenger = null;
        AutoCloseable redisCloseable = null;
        Messenger rpcMessenger = null;
        AutoCloseable rpcCloseable = null;

        if (priority == MessengerTransportBootstrap.TransportPriority.RPC_SERVER_FIRST
            || priority == MessengerTransportBootstrap.TransportPriority.RPC_SERVER_ONLY) {
            MessengerTransportBootstrap.Result result = bootstrapSharedTransport(
                transportConfig,
                platformId,
                logger,
                services,
                priority
            );
            if (result != null) {
                rpcMessenger = result.messenger();
                rpcCloseable = result.closeable();
            }
        } else if (priority != MessengerTransportBootstrap.TransportPriority.PLUGIN_ONLY) {
            MessengerTransportBootstrap.Result result = bootstrapSharedTransport(
                transportConfig,
                platformId,
                logger,
                services,
                priority
            );
            if (result != null && result.usingRedis()) {
                redisMessenger = result.messenger();
                redisCloseable = result.closeable();
            }
        }

        Messenger effective = TransportSelectionPlanner.select(
            priority,
            logger,
            services,
            inMemoryMessenger,
            pluginEffective,
            redisMessenger,
            rpcMessenger
        );
        services.register(Messenger.class, effective);

        return new Result(
            platformId,
            names.serverName(),
            names.proxyServerName(),
            priority,
            pluginMessenger,
            pluginEffective,
            redisMessenger,
            rpcMessenger,
            effective,
            queueCloseable,
            redisCloseable,
            rpcCloseable
        );
    }

    private static DecoratedPluginTransport decoratePluginTransport(
        Messenger pluginMessenger,
        TransportContext transportContext,
        PluginHooks pluginHooks
    ) {
        if (pluginHooks == null || pluginHooks.transportDecorator() == null) {
            return DecoratedPluginTransport.passthrough(pluginMessenger);
        }
        DecoratedPluginTransport decorated = pluginHooks.transportDecorator().decorate(pluginMessenger, transportContext, pluginHooks);
        return decorated != null ? decorated : DecoratedPluginTransport.passthrough(pluginMessenger);
    }

    private static boolean shouldInitializePluginTransport(MessengerTransportBootstrap.TransportPriority priority) {
        return priority != MessengerTransportBootstrap.TransportPriority.REDIS_ONLY
            && priority != MessengerTransportBootstrap.TransportPriority.RPC_SERVER_ONLY;
    }

    private static MessengerTransportBootstrap.Result bootstrapSharedTransport(
        YamlConfig transportConfig,
        PlatformId platformId,
        Logger logger,
        ServiceRegistry services,
        MessengerTransportBootstrap.TransportPriority priority
    ) {
        Messenger previous = services.find(Messenger.class).orElse(null);
        return distinctTransport(
            previous,
            MessengerTransportBootstrap.bootstrap(transportConfig, platformId, logger, services, priority)
        );
    }

    private static MessengerTransportBootstrap.Result distinctTransport(Messenger previous, MessengerTransportBootstrap.Result result) {
        if (result == null || result.messenger() == null) {
            return null;
        }
        if (result.messenger() == previous) {
            return null;
        }
        return result;
    }
}
