package de.t14d3.rapunzellib.network.runtime;

import de.t14d3.rapunzellib.network.Messenger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Describes a network link with its transport kind and associated messenger.
 *
 * @param kind      the transport kind
 * @param messenger the messenger for this link
 */
public record NetworkLink(
    @NotNull NetworkLinkKind kind,
    @NotNull Messenger messenger
) {
    public NetworkLink {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(messenger, "messenger");
    }

    /**
     * Returns the local server name from the link's messenger.
     *
     * @return the local server name
     */
    public @NotNull String localName() {
        return messenger.getServerName();
    }

    /**
     * Returns the proxy server name from the link's messenger.
     *
     * @return the proxy server name
     */
    public @NotNull String proxyName() {
        return messenger.getProxyServerName();
    }
}
