package de.t14d3.rapunzellib.network.runtime;

import de.t14d3.rapunzellib.network.Messenger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link NetworkRuntime}.
 *
 * @param localRole      the role of this node (PROXY or BACKEND)
 * @param localName      the name of this node
 * @param proxyName      the name of the proxy server
 * @param canonicalLink  the primary network link
 * @param bootstrapLink  optional bootstrap link for initialization
 * @param messenger      the effective messenger
 */
public record DefaultNetworkRuntime(
    @NotNull NetworkNodeRole localRole,
    @NotNull String localName,
    @NotNull String proxyName,
    @NotNull NetworkLink canonicalLink,
    @NotNull Optional<NetworkLink> bootstrapLink,
    @NotNull Messenger messenger
) implements NetworkRuntime {
    public DefaultNetworkRuntime {
        Objects.requireNonNull(localRole, "localRole");
        Objects.requireNonNull(localName, "localName");
        Objects.requireNonNull(proxyName, "proxyName");
        Objects.requireNonNull(canonicalLink, "canonicalLink");
        bootstrapLink = bootstrapLink != null ? bootstrapLink : Optional.empty();
        Objects.requireNonNull(messenger, "messenger");
    }
}
