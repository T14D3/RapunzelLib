package de.t14d3.rapunzellib.network.runtime;

import de.t14d3.rapunzellib.network.Messenger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

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
