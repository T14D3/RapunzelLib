package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.bootstrap.BackendTransportBootstrap;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Factory for creating plugin transport decorators that wrap a {@link Messenger}
 * in a {@link DbQueuedMessenger} for store-and-forward queueing.
 * <p>
 * Provides convenience methods for creating standard and customized
 * {@link BackendTransportBootstrap.PluginTransportDecorator} instances
 * and fully-configured {@link BackendTransportBootstrap.PluginHooks}.
 * </p>
 */
public final class NetworkQueueTransportDecorator {
    private NetworkQueueTransportDecorator() {
    }

    /**
     * Creates a transport decorator that wraps a messenger in a queued messenger using the given listener.
     *
     * @param listener the lifecycle listener for outbox events, or null for no-op
     * @return a new plugin transport decorator
     */
    public static BackendTransportBootstrap.PluginTransportDecorator create(DbQueuedMessenger.Listener listener) {
        return (pluginMessenger, context, pluginHooks) -> {
            NetworkQueueBootstrap.Result queued = NetworkQueueBootstrap.wrapIfEnabled(
                pluginMessenger,
                context.transportConfig(),
                context.scheduler(),
                context.logger(),
                context.ownerId(),
                pluginHooks != null ? pluginHooks.allServersSupplier() : null,
                pluginHooks != null ? pluginHooks.canSendToServerOverride() : null,
                listener
            );
            return new BackendTransportBootstrap.DecoratedPluginTransport(queued.messenger(), queued.closeable());
        };
    }

    /**
     * Creates standard {@link BackendTransportBootstrap.PluginHooks} with queueing enabled and a messenger factory.
     *
     * @param messengerFactory supplier for the underlying messenger
     * @return standard plugin hooks with queueing decorator
     */
    public static BackendTransportBootstrap.PluginHooks pluginHooks(Supplier<? extends Messenger> messengerFactory) {
        return BackendTransportBootstrap.PluginHooks.standard(messengerFactory)
            .withTransportDecorator(create(null));
    }

    /**
     * Creates fully customized {@link BackendTransportBootstrap.PluginHooks} with queueing.
     *
     * @param messengerFactory         supplier for the underlying messenger
     * @param allServersSupplier       supplier for the list of all known servers
     * @param canSendToServerOverride  predicate to override send-to-server decisions
     * @param listener                 the lifecycle listener
     * @param onPluginEffective        callback invoked with the original and effective messenger
     * @return custom plugin hooks with queueing decorator
     */
    public static BackendTransportBootstrap.PluginHooks pluginHooks(
        Supplier<? extends Messenger> messengerFactory,
        Supplier<java.util.List<String>> allServersSupplier,
        Predicate<String> canSendToServerOverride,
        DbQueuedMessenger.Listener listener,
        BiConsumer<Messenger, Messenger> onPluginEffective
    ) {
        return new BackendTransportBootstrap.PluginHooks(
            messengerFactory,
            allServersSupplier,
            canSendToServerOverride,
            create(listener),
            onPluginEffective
        );
    }
}
