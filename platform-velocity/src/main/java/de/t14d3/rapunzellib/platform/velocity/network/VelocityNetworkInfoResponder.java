package de.t14d3.rapunzellib.platform.velocity.network;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import de.t14d3.rapunzellib.network.info.NetworkInfoRpc;
import de.t14d3.rapunzellib.network.info.NetworkPlayerInfo;
import de.t14d3.rapunzellib.network.rpc.RpcChannels;
import de.t14d3.rapunzellib.network.rpc.RpcRequest;
import de.t14d3.rapunzellib.network.rpc.RpcResponse;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

public final class VelocityNetworkInfoResponder implements AutoCloseable {
    private final Logger logger;
    private final Gson gson;
    private final NetworkEventBus bus;
    private final NetworkEventBus.Subscription subscription;
    private final ProxyServer proxy;

    public VelocityNetworkInfoResponder(Messenger messenger, ProxyServer proxy, Logger logger) {
        this.proxy = Objects.requireNonNull(proxy, "proxy");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.gson = JsonCodecs.gson();
        this.bus = new NetworkEventBus(Objects.requireNonNull(messenger, "messenger"), gson);

        this.subscription = bus.register(
            RpcChannels.REQUEST,
            RpcRequest.class,
            this::handleRequest
        );
    }

    private void handleRequest(RpcRequest request, String sourceServer) {
        if (request == null || request.requestId() == null) return;
        if (sourceServer == null || sourceServer.isBlank()) return;
        if (!NetworkInfoRpc.SERVICE.equals(request.service())) return;

        try {
            switch (request.method()) {
                case NetworkInfoRpc.WHO_AM_I -> sendOk(request.requestId(), sourceServer, sourceServer);
                case NetworkInfoRpc.LIST_SERVERS -> sendOk(request.requestId(), sourceServer, listServers());
                case NetworkInfoRpc.LIST_PLAYERS -> sendOk(request.requestId(), sourceServer, listPlayers());
                default -> sendError(request.requestId(), sourceServer, "Unknown method: " + request.method());
            }
        } catch (Exception e) {
            logger.warn("Failed to handle network info request from {}", sourceServer, e);
            sendError(request.requestId(), sourceServer, e.getMessage());
        }
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

    private void sendOk(String requestId, String targetServer, Object result) {
        bus.sendToServer(
            RpcChannels.RESPONSE,
            targetServer,
            new RpcResponse(requestId, true, gson.toJsonTree(result), null, System.currentTimeMillis())
        );
    }

    private void sendError(String requestId, String targetServer, String error) {
        bus.sendToServer(
            RpcChannels.RESPONSE,
            targetServer,
            new RpcResponse(requestId, false, null, error, System.currentTimeMillis())
        );
    }

    @Override
    public void close() {
        subscription.close();
    }
}
