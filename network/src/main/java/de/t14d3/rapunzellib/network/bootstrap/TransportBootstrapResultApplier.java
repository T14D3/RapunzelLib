package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.remote.RemoteHandlerRegistrar;
import de.t14d3.rapunzellib.network.rpcserver.RpcClientMessenger;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntime;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeClassifier;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Applies the result of a backend transport bootstrap to a RapunzelContext.
 *
 * <p>Registers the effective messenger, network runtime, gateway, and any closeable
 * resources into the context.
 */
public final class TransportBootstrapResultApplier {
    private TransportBootstrapResultApplier() {
    }

    /**
     * Applies the transport bootstrap result to the Rapunzel context.
     *
     * @param ctx       the Rapunzel context
     * @param logger    the logger
     * @param transport the transport bootstrap result
     * @return the effective messenger
     */
    public static Messenger apply(RapunzelContext ctx, Logger logger, BackendTransportBootstrap.Result transport) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(transport, "transport");

        if (transport.rpcMessenger() instanceof RpcClientMessenger rpcClient) {
            ctx.register(RpcClientMessenger.class, rpcClient);
            logger.info("[Network] Using RpcClientMessenger for RPC transport");
        }

        registerCloseable(ctx, transport.queueCloseable());
        registerCloseable(ctx, transport.redisCloseable());
        registerCloseable(ctx, transport.rpcCloseable());

        NetworkRuntime networkRuntime = NetworkRuntimeClassifier.classify(transport);
        ctx.register(NetworkRuntime.class, networkRuntime);
        DefaultNetworkRuntimeGateway gateway = new DefaultNetworkRuntimeGateway(networkRuntime, ctx.scheduler(), logger);
        ctx.registerLinked(
            DefaultNetworkRuntimeGateway.class,
            gateway,
            NetworkRuntimeGateway.class
        );

        RemoteHandlerRegistrar.install(ctx, gateway);

        Messenger effective = transport.effectiveMessenger();
        ctx.register(Messenger.class, effective);
        logger.info(
            "[Network] Canonical link={} localRole={} localName={} proxyName={}",
            networkRuntime.canonicalLink().kind(),
            networkRuntime.localRole(),
            networkRuntime.localName(),
            networkRuntime.proxyName()
        );
        networkRuntime.bootstrapLink().ifPresent(link -> logger.info("[Network] Bootstrap link={}", link.kind()));
        return effective;
    }

    private static void registerCloseable(RapunzelContext ctx, AutoCloseable closeable) {
        if (closeable != null) {
            ctx.registerCloseable(closeable);
        }
    }
}
