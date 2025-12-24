package de.t14d3.rapunzellib.network.info;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface NetworkInfoService {
    /**
     * Returns the name this server is known as on the network (e.g. the backend name as configured in the Velocity
     * config). Backend servers typically do not know this locally and must ask the proxy.
     */
    CompletableFuture<String> networkServerName();

    CompletableFuture<List<String>> servers();

    CompletableFuture<List<NetworkPlayerInfo>> players();
}

