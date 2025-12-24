package de.t14d3.rapunzellib.platform.velocity.network;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.info.NetworkPlayerInfo;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class VelocityNetworkInfoService implements NetworkInfoService {
    private final Messenger messenger;
    private final ProxyServer proxy;

    public VelocityNetworkInfoService(Messenger messenger, ProxyServer proxy) {
        this.messenger = Objects.requireNonNull(messenger, "messenger");
        this.proxy = Objects.requireNonNull(proxy, "proxy");
    }

    @Override
    public CompletableFuture<String> networkServerName() {
        return CompletableFuture.completedFuture(messenger.getServerName());
    }

    @Override
    public CompletableFuture<List<String>> servers() {
        return CompletableFuture.completedFuture(proxy.getAllServers().stream()
            .map(rs -> rs.getServerInfo().getName())
            .toList());
    }

    @Override
    public CompletableFuture<List<NetworkPlayerInfo>> players() {
        return CompletableFuture.completedFuture(proxy.getAllPlayers().stream()
            .map(this::toPlayerInfo)
            .toList());
    }

    private NetworkPlayerInfo toPlayerInfo(Player player) {
        String serverName = player.getCurrentServer()
            .map(sc -> sc.getServerInfo().getName())
            .orElse(null);
        return new NetworkPlayerInfo(player.getUniqueId(), player.getUsername(), serverName);
    }
}

