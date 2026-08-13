package de.t14d3.rapunzellib.network.remote.handler;

import de.t14d3.rapunzellib.network.remote.rpc.ProxyServiceMethods;
import de.t14d3.rapunzellib.network.remote.rpc.Requests;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ProxyRpcHandlerRegistrar {
    private ProxyRpcHandlerRegistrar() {}

    @FunctionalInterface
    public interface PlayerConnector {
        CompletableFuture<Boolean> connect(UUID playerUuid, String targetServer);
    }

    /**
     * Registers the proxy-side RPC handlers: {@code proxy/connectPlayer}
     * (executes the actual server connect and stores the deferred teleport
     * location) and {@code proxy/pollDeferredTeleport} (lets backends claim a
     * deferred teleport after the player joined).
     *
     * @param gateway   the gateway to register the handlers on
     * @param connector the platform connector performing the actual connect
     * @return a subscription closing both registered handlers
     */
    public static @NotNull NetworkRuntimeGateway.Subscription register(@NotNull NetworkRuntimeGateway gateway,
                                                                       @NotNull PlayerConnector connector) {
        Objects.requireNonNull(gateway, "gateway");
        Objects.requireNonNull(connector, "connector");

        NetworkRuntimeGateway.Subscription connectSubscription =
            gateway.register(ProxyServiceMethods.PROXY_CONNECT_PLAYER, (req, source) -> {
                if (req == null || req.uuid() == null || req.targetServer() == null) {
                    return CompletableFuture.completedFuture(new Requests.BooleanResult(false));
                }

                return connector.connect(req.uuid(), req.targetServer())
                    .thenApply(success -> {
                        if (success && req.hasLocation() && req.world() != null) {
                            RWorldRef world = req.world();
                            RLocation loc = new RLocation(world, req.x(), req.y(), req.z(), req.yaw(), req.pitch());
                            DeferredTeleportStore.store(req.uuid(), loc);
                        }
                        return new Requests.BooleanResult(success);
                    });
            });

        NetworkRuntimeGateway.Subscription pollSubscription =
            gateway.register(ProxyServiceMethods.PROXY_POLL_DEFERRED_TELEPORT, (req, source) -> {
                if (req == null || req.playerId() == null) {
                    return CompletableFuture.completedFuture(new Requests.PollDeferredTeleportResult(null));
                }
                return CompletableFuture.completedFuture(
                    new Requests.PollDeferredTeleportResult(DeferredTeleportStore.poll(req.playerId())));
            });

        return new CombinedSubscription(connectSubscription, pollSubscription);
    }

    private static final class CombinedSubscription implements NetworkRuntimeGateway.Subscription {
        private final NetworkRuntimeGateway.Subscription[] subscriptions;

        private CombinedSubscription(NetworkRuntimeGateway.Subscription... subscriptions) {
            this.subscriptions = subscriptions;
        }

        @Override
        public void close() {
            for (NetworkRuntimeGateway.Subscription subscription : subscriptions) {
                try {
                    subscription.close();
                } catch (Exception e) {
                    // Closing the remaining subscriptions must not be blocked by one failure.
                }
            }
        }
    }
}
