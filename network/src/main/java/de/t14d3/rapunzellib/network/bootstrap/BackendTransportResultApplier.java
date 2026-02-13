package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.network.Messenger;
import org.slf4j.Logger;

public final class BackendTransportResultApplier {
    private BackendTransportResultApplier() {
    }

    public static Messenger apply(RapunzelContext ctx, Logger logger, BackendTransportBootstrap.Result transport) {
        return TransportBootstrapResultApplier.apply(ctx, logger, transport);
    }
}
