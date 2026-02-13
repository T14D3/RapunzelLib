package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.rpcserver.RpcClientMessenger;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntime;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeClassifier;
import org.slf4j.Logger;

import java.util.Objects;

public final class TransportBootstrapResultApplier {
    private TransportBootstrapResultApplier() {
    }

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
        ctx.registerLinked(
            DefaultNetworkRuntimeGateway.class,
            new DefaultNetworkRuntimeGateway(networkRuntime, ctx.scheduler(), logger),
            NetworkRuntimeGateway.class
        );

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
