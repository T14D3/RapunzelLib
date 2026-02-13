package de.t14d3.rapunzellib.network.runtime;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkDefaults;
import de.t14d3.rapunzellib.network.bootstrap.BackendTransportBootstrap;
import de.t14d3.rapunzellib.network.bootstrap.MessengerTransportBootstrap;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class NetworkRuntimeClassifier {
    private NetworkRuntimeClassifier() {
    }

    public static @NotNull NetworkRuntime classify(@NotNull BackendTransportBootstrap.Result transport) {
        Objects.requireNonNull(transport, "transport");

        NetworkLink canonicalLink = canonicalLink(transport);
        Messenger bootstrapMessenger = pluginBootstrapMessenger(transport);
        Optional<NetworkLink> bootstrapLink = Optional.ofNullable(bootstrapMessenger)
            .filter(messenger -> messenger != canonicalLink.messenger())
            .map(messenger -> new NetworkLink(NetworkLinkKind.PLUGIN_MESSAGING, messenger));

        return new DefaultNetworkRuntime(
            nodeRole(transport.platformId()),
            resolveLocalName(transport, canonicalLink),
            resolveProxyName(transport, canonicalLink),
            canonicalLink,
            bootstrapLink,
            transport.effectiveMessenger()
        );
    }

    public static @NotNull NetworkRuntime fallback(
        @NotNull PlatformId platformId,
        @NotNull Messenger messenger
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(messenger, "messenger");

        return new DefaultNetworkRuntime(
            nodeRole(platformId),
            nonBlankOrDefault(messenger.getServerName(), "unknown"),
            nonBlankOrDefault(messenger.getProxyServerName(), NetworkDefaults.DEFAULT_PROXY_SERVER_NAME),
            new NetworkLink(NetworkLinkKind.IN_MEMORY, messenger),
            Optional.empty(),
            messenger
        );
    }

    private static @NotNull String resolveLocalName(
        @NotNull BackendTransportBootstrap.Result transport,
        @NotNull NetworkLink canonicalLink
    ) {
        String configured = transport.serverName();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        String canonical = canonicalLink.localName();
        if (canonical != null && !canonical.isBlank()) {
            return canonical;
        }

        Messenger effective = transport.effectiveMessenger();
        String effectiveName = effective != null ? effective.getServerName() : null;
        return nonBlankOrDefault(effectiveName, "unknown");
    }

    private static @NotNull String resolveProxyName(
        @NotNull BackendTransportBootstrap.Result transport,
        @NotNull NetworkLink canonicalLink
    ) {
        String configured = transport.proxyServerName();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        String canonical = canonicalLink.proxyName();
        if (canonical != null && !canonical.isBlank()) {
            return canonical;
        }

        Messenger effective = transport.effectiveMessenger();
        String effectiveName = effective != null ? effective.getProxyServerName() : null;
        return nonBlankOrDefault(effectiveName, NetworkDefaults.DEFAULT_PROXY_SERVER_NAME);
    }

    private static @NotNull String nonBlankOrDefault(String value, @NotNull String fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static @NotNull NetworkLink canonicalLink(BackendTransportBootstrap.Result transport) {
        MessengerTransportBootstrap.TransportPriority priority = transport.priority();

        NetworkLink link = switch (priority) {
            case RPC_SERVER_FIRST, RPC_SERVER_ONLY -> firstDedicated(
                link(NetworkLinkKind.RPC, transport.rpcMessenger()),
                link(NetworkLinkKind.REDIS_PUBSUB, transport.redisMessenger())
            );
            case REDIS_FIRST, REDIS_ONLY -> firstDedicated(
                link(NetworkLinkKind.REDIS_PUBSUB, transport.redisMessenger()),
                link(NetworkLinkKind.RPC, transport.rpcMessenger())
            );
            case PLUGIN_FIRST, PLUGIN_ONLY -> firstDedicated(
                link(NetworkLinkKind.RPC, transport.rpcMessenger()),
                link(NetworkLinkKind.REDIS_PUBSUB, transport.redisMessenger())
            );
        };

        if (link != null) {
            return link;
        }

        Messenger bootstrapMessenger = pluginBootstrapMessenger(transport);
        if (bootstrapMessenger != null) {
            return new NetworkLink(NetworkLinkKind.PLUGIN_MESSAGING, bootstrapMessenger);
        }

        return new NetworkLink(NetworkLinkKind.IN_MEMORY, transport.effectiveMessenger());
    }

    private static NetworkLink firstDedicated(NetworkLink first, NetworkLink second) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return null;
    }

    private static Messenger pluginBootstrapMessenger(BackendTransportBootstrap.Result transport) {
        if (transport.pluginEffective() != null) {
            return transport.pluginEffective();
        }
        return transport.pluginMessenger();
    }

    private static NetworkLink link(NetworkLinkKind kind, Messenger messenger) {
        if (messenger == null) {
            return null;
        }
        return new NetworkLink(kind, messenger);
    }

    private static @NotNull NetworkNodeRole nodeRole(@NotNull PlatformId platformId) {
        return platformId == PlatformId.VELOCITY ? NetworkNodeRole.PROXY : NetworkNodeRole.BACKEND;
    }
}
