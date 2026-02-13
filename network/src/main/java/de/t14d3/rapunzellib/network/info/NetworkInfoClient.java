package de.t14d3.rapunzellib.network.info;

import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.rpc.RpcClient;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class NetworkInfoClient implements NetworkInfoService, AutoCloseable {
    private final RpcClient rpc;
    private final Object networkServerNameLock = new Object();
    private volatile String cachedNetworkServerName;
    private volatile CompletableFuture<String> inFlightNetworkServerName;

    public NetworkInfoClient(Messenger messenger, Scheduler scheduler, Logger logger) {
        this(DefaultNetworkRuntimeGateway.compatibility(messenger), scheduler, logger, Duration.ofSeconds(3));
    }

    public NetworkInfoClient(Messenger messenger, Scheduler scheduler, Logger logger, Duration requestTimeout) {
        this(DefaultNetworkRuntimeGateway.compatibility(messenger), scheduler, logger, requestTimeout);
    }

    public NetworkInfoClient(NetworkRuntimeGateway gateway, Scheduler scheduler, Logger logger) {
        this(gateway, scheduler, logger, Duration.ofSeconds(3));
    }

    public NetworkInfoClient(NetworkRuntimeGateway gateway, Scheduler scheduler, Logger logger, Duration requestTimeout) {
        Objects.requireNonNull(requestTimeout, "requestTimeout");
        this.rpc = new RpcClient(Objects.requireNonNull(gateway, "gateway"), scheduler, logger, requestTimeout);
    }

    @Override
    public CompletableFuture<String> networkServerName() {
        String cached = cachedNetworkServerName;
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        synchronized (networkServerNameLock) {
            cached = cachedNetworkServerName;
            if (cached != null) {
                return CompletableFuture.completedFuture(cached);
            }

            CompletableFuture<String> inFlight = inFlightNetworkServerName;
            if (inFlight != null) {
                return inFlight;
            }

            CompletableFuture<String> started = rpc.callProxy(NetworkInfoRpc.WHO_AM_I_METHOD, null)
                .thenApply(name -> {
                    if (name == null || name.isBlank()) {
                        throw new IllegalStateException("Proxy returned an empty server name");
                    }
                    cachedNetworkServerName = name;
                    return name;
                });

            inFlightNetworkServerName = started;
            started.whenComplete((result, error) -> {
                synchronized (networkServerNameLock) {
                    if (inFlightNetworkServerName == started) {
                        inFlightNetworkServerName = null;
                    }
                }
            });

            return started;
        }
    }

    @Override
    public CompletableFuture<List<String>> servers() {
        return rpc.callProxy(NetworkInfoRpc.LIST_SERVERS_METHOD, null);
    }

    @Override
    public CompletableFuture<List<NetworkPlayerInfo>> players() {
        return rpc.callProxy(NetworkInfoRpc.LIST_PLAYERS_METHOD, null);
    }

    @Override
    public void close() {
        rpc.close();
    }
}
