package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.bootstrap.BackendTransportBootstrap;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class NetworkQueueTransportDecorator {
    private NetworkQueueTransportDecorator() {
    }

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

    public static BackendTransportBootstrap.PluginHooks pluginHooks(Supplier<? extends Messenger> messengerFactory) {
        return BackendTransportBootstrap.PluginHooks.standard(messengerFactory)
            .withTransportDecorator(create(null));
    }

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
