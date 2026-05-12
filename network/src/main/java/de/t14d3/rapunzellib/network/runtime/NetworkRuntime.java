package de.t14d3.rapunzellib.network.runtime;

import de.t14d3.rapunzellib.network.Messenger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Describes the network runtime environment for the current server.
 *
 * <p>Provides information about the local node's role, identity, and the
 * network links available for communication.
 */
public interface NetworkRuntime {
    /**
     * Returns the role of this node (PROXY or BACKEND).
     *
     * @return the local node role
     */
    @NotNull NetworkNodeRole localRole();

    /**
     * Returns the name of this node on the network.
     *
     * @return the local server name
     */
    @NotNull String localName();

    /**
     * Returns the name of the proxy server.
     *
     * @return the proxy server name
     */
    @NotNull String proxyName();

    /**
     * Returns the canonical (primary) network link.
     *
     * @return the canonical link
     */
    @NotNull NetworkLink canonicalLink();

    /**
     * Returns an optional bootstrap link used during initialization.
     *
     * @return the bootstrap link, if present
     */
    @NotNull Optional<NetworkLink> bootstrapLink();

    /**
     * Returns the effective messenger for this runtime.
     *
     * @return the messenger
     */
    @NotNull Messenger messenger();

    /**
     * Returns the messenger from the canonical link.
     *
     * @return the canonical messenger
     */
    default @NotNull Messenger canonicalMessenger() {
        return canonicalLink().messenger();
    }

    /**
     * Returns the bootstrap messenger, if available.
     *
     * @return an optional messenger from the bootstrap link
     */
    default @NotNull Optional<Messenger> bootstrapMessenger() {
        return bootstrapLink().map(NetworkLink::messenger);
    }
}
