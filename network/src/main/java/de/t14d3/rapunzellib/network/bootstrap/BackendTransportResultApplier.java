package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.network.Messenger;
import org.slf4j.Logger;

/**
 * Convenience wrapper around {@link TransportBootstrapResultApplier}.
 *
 * @deprecated Use {@link TransportBootstrapResultApplier#apply} directly.
 */
@Deprecated
public final class BackendTransportResultApplier {
    private BackendTransportResultApplier() {
    }

    /**
     * Applies the transport bootstrap result, delegating to {@link TransportBootstrapResultApplier}.
     *
     * @param ctx       the Rapunzel context
     * @param logger    the logger
     * @param transport the transport bootstrap result
     * @return the effective messenger
     */
    public static Messenger apply(RapunzelContext ctx, Logger logger, BackendTransportBootstrap.Result transport) {
        return TransportBootstrapResultApplier.apply(ctx, logger, transport);
    }
}
