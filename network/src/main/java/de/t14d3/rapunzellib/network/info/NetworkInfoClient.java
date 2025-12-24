package de.t14d3.rapunzellib.network.info;

import com.google.gson.reflect.TypeToken;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.rpc.RpcClient;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class NetworkInfoClient implements NetworkInfoService, AutoCloseable {
    private static final Type SERVERS_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    private static final Type PLAYERS_LIST_TYPE = new TypeToken<List<NetworkPlayerInfo>>() {
    }.getType();

    private final RpcClient rpc;
    private final Object networkServerNameLock = new Object();
    private volatile String cachedNetworkServerName;
    private volatile CompletableFuture<String> inFlightNetworkServerName;

    public NetworkInfoClient(Messenger messenger, Scheduler scheduler, Logger logger) {
        this(messenger, scheduler, logger, Duration.ofSeconds(3));
    }

    public NetworkInfoClient(Messenger messenger, Scheduler scheduler, Logger logger, Duration requestTimeout) {
        Objects.requireNonNull(requestTimeout, "requestTimeout");
        this.rpc = new RpcClient(messenger, scheduler, logger, requestTimeout);
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

            CompletableFuture<String> started = rpc.callProxy(NetworkInfoRpc.SERVICE, NetworkInfoRpc.WHO_AM_I, null, String.class)
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
        return rpc.callProxy(NetworkInfoRpc.SERVICE, NetworkInfoRpc.LIST_SERVERS, null, SERVERS_LIST_TYPE);
    }

    @Override
    public CompletableFuture<List<NetworkPlayerInfo>> players() {
        return rpc.callProxy(NetworkInfoRpc.SERVICE, NetworkInfoRpc.LIST_PLAYERS, null, PLAYERS_LIST_TYPE);
    }

    @Override
    public void close() {
        rpc.close();
    }
}
