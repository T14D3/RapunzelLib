package de.t14d3.rapunzellib.network.runtime;

import de.t14d3.rapunzellib.network.Messenger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record NetworkLink(
    @NotNull NetworkLinkKind kind,
    @NotNull Messenger messenger
) {
    public NetworkLink {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(messenger, "messenger");
    }

    public @NotNull String localName() {
        return messenger.getServerName();
    }

    public @NotNull String proxyName() {
        return messenger.getProxyServerName();
    }
}
