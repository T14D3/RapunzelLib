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
import java.util.function.BiFunction;

public final class ProxyRpcHandlerRegistrar {
    private ProxyRpcHandlerRegistrar() {}

    @FunctionalInterface
    public interface PlayerConnector {
        CompletableFuture<Boolean> connect(UUID playerUuid, String targetServer);
    }

    public static void register(@NotNull NetworkRuntimeGateway gateway,
                                 @NotNull PlayerConnector connector) {
        Objects.requireNonNull(gateway, "gateway");
        Objects.requireNonNull(connector, "connector");

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
    }
}
