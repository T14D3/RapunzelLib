package de.t14d3.rapunzellib.network.runtime;

import de.t14d3.rapunzellib.network.Messenger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface NetworkRuntime {
    @NotNull NetworkNodeRole localRole();

    @NotNull String localName();

    @NotNull String proxyName();

    @NotNull NetworkLink canonicalLink();

    @NotNull Optional<NetworkLink> bootstrapLink();

    @NotNull Messenger messenger();

    default @NotNull Messenger canonicalMessenger() {
        return canonicalLink().messenger();
    }

    default @NotNull Optional<Messenger> bootstrapMessenger() {
        return bootstrapLink().map(NetworkLink::messenger);
    }
}
