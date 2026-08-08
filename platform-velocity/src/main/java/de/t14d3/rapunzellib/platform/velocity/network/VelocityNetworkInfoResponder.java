package de.t14d3.rapunzellib.platform.velocity.network;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.network.info.NetworkInfoRpc;
import de.t14d3.rapunzellib.network.info.NetworkPlayerInfo;
import de.t14d3.rapunzellib.network.runtime.NetworkPath;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class VelocityNetworkInfoResponder implements AutoCloseable {
    private static final Duration BACKEND_QUERY_TIMEOUT = Duration.ofSeconds(2);

    private final NetworkRuntimeGateway.Subscription whoAmISubscription;
    private final NetworkRuntimeGateway.Subscription serversSubscription;
    private final NetworkRuntimeGateway.Subscription playersSubscription;
    private final ProxyServer proxy;
    private final NetworkRuntimeGateway gateway;

    public VelocityNetworkInfoResponder(NetworkRuntimeGateway gateway, ProxyServer proxy, Logger logger) {
        this.proxy = Objects.requireNonNull(proxy, "proxy");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        Objects.requireNonNull(logger, "logger");
        NetworkRuntimeGateway runtimeGateway = gateway;
        this.whoAmISubscription = runtimeGateway.register(
            NetworkInfoRpc.WHO_AM_I_METHOD,
            NetworkRuntimeGateway.RpcHandler.sync((_ignored, sourceServer) -> sourceServer)
        );
        this.serversSubscription = runtimeGateway.register(
            NetworkInfoRpc.LIST_SERVERS_METHOD,
            NetworkRuntimeGateway.RpcHandler.sync((_ignored, sourceServer) -> listServers())
        );
        this.playersSubscription = runtimeGateway.register(
            NetworkInfoRpc.LIST_PLAYERS_METHOD,
            (_ignored, sourceServer) -> listPlayers()
        );
    }

    private List<String> listServers() {
        return proxy.getAllServers().stream()
            .map(rs -> rs.getServerInfo().getName())
            .toList();
    }

    /**
     * Returns all players currently online on the network.
     *
     * <p>Players connected through the proxy are listed directly. Because
     * backends may also host players that connect directly to the backend (no
     * proxy traversal), each registered backend is queried for its local
     * players and the results are merged. Backend queries that time out or
     * fail are skipped.</p>
     */
    private CompletableFuture<List<NetworkPlayerInfo>> listPlayers() {
        List<NetworkPlayerInfo> merged = new ArrayList<>();
        for (Player player : proxy.getAllPlayers()) {
            NetworkPlayerInfo info = toPlayerInfo(player);
            if (info != null) {
                merged.add(info);
            }
        }

        List<CompletableFuture<List<NetworkPlayerInfo>>> backendQueries = new ArrayList<>();
        for (var server : proxy.getAllServers()) {
            String serverName = server.getServerInfo().getName();
            if (serverName == null || serverName.isBlank()) continue;
            if (serverName.equalsIgnoreCase(gateway.runtime().localName())) continue;
            try {
                backendQueries.add(
                    gateway.call(NetworkPath.server(serverName), NetworkInfoRpc.LIST_LOCAL_PLAYERS_METHOD, null,
                            BACKEND_QUERY_TIMEOUT)
                        .exceptionally(_ignored -> List.of())
                );
            } catch (Exception e) {
                // Gateway cannot reach this backend (unsupported, disconnected, ...) - skip it.
            }
        }

        if (backendQueries.isEmpty()) {
            return CompletableFuture.completedFuture(merged);
        }

        return CompletableFuture.allOf(backendQueries.toArray(new CompletableFuture<?>[0]))
            .thenApply(_ignored -> {
                for (CompletableFuture<List<NetworkPlayerInfo>> query : backendQueries) {
                    List<NetworkPlayerInfo> players = query.join();
                    if (players == null) continue;
                    for (NetworkPlayerInfo info : players) {
                        if (info != null && info.uuid() != null && info.name() != null && !info.name().isBlank()) {
                            merged.add(info);
                        }
                    }
                }
                return merged;
            });
    }

    private NetworkPlayerInfo toPlayerInfo(Player player) {
        if (player == null) return null;
        String serverName = player.getCurrentServer()
            .map(sc -> sc.getServerInfo().getName())
            .orElse(null);
        if (serverName == null || serverName.isBlank()) return null;
        return new NetworkPlayerInfo(player.getUniqueId(), player.getUsername(), serverName);
    }

    @Override
    public void close() {
        whoAmISubscription.close();
        serversSubscription.close();
        playersSubscription.close();
    }
}
