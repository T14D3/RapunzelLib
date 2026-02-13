package de.t14d3.rapunzellib.platform.velocity.network;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.network.info.NetworkInfoRpc;
import de.t14d3.rapunzellib.network.info.NetworkPlayerInfo;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

public final class VelocityNetworkInfoResponder implements AutoCloseable {
    private final NetworkRuntimeGateway.Subscription whoAmISubscription;
    private final NetworkRuntimeGateway.Subscription serversSubscription;
    private final NetworkRuntimeGateway.Subscription playersSubscription;
    private final ProxyServer proxy;

    public VelocityNetworkInfoResponder(NetworkRuntimeGateway gateway, ProxyServer proxy, Logger logger) {
        this.proxy = Objects.requireNonNull(proxy, "proxy");
        Objects.requireNonNull(logger, "logger");
        NetworkRuntimeGateway runtimeGateway = Objects.requireNonNull(gateway, "gateway");
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
            NetworkRuntimeGateway.RpcHandler.sync((_ignored, sourceServer) -> listPlayers())
        );
    }

    private List<String> listServers() {
        return proxy.getAllServers().stream()
            .map(rs -> rs.getServerInfo().getName())
            .toList();
    }

    private List<NetworkPlayerInfo> listPlayers() {
        return proxy.getAllPlayers().stream()
            .map(this::toPlayerInfo)
            .toList();
    }

    private NetworkPlayerInfo toPlayerInfo(Player player) {
        String serverName = player.getCurrentServer()
            .map(sc -> sc.getServerInfo().getName())
            .orElse(null);
        return new NetworkPlayerInfo(player.getUniqueId(), player.getUsername(), serverName);
    }

    @Override
    public void close() {
        whoAmISubscription.close();
        serversSubscription.close();
        playersSubscription.close();
    }
}
