package de.t14d3.rapunzellib.network.info;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for querying network topology and player information.
 */
public interface NetworkInfoService {
    /**
     * Returns the name this server is known as on the network (e.g. the backend name as configured in the Velocity
     * config). Backend servers typically do not know this locally and must ask the proxy.
     *
     * @return future resolving to the network server name
     */
    CompletableFuture<String> networkServerName();

    /**
     * Returns a list of all server names on the network.
     *
     * @return future resolving to the list of server names
     */
    CompletableFuture<List<String>> servers();

    /**
     * Returns info about all players currently on the network.
     *
     * @return future resolving to the list of player info
     */
    CompletableFuture<List<NetworkPlayerInfo>> players();
}

