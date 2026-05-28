package de.t14d3.rapunzellib.network.remote.resolution;

import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.info.NetworkPlayerInfo;
import de.t14d3.rapunzellib.network.remote.proxy.RemotePlayer;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RProxyPlayer;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NetworkedPlayers implements Players {
    private static final Logger logger = LoggerFactory.getLogger(NetworkedPlayers.class);
    private static final long RESOLVE_TIMEOUT_MS = 3000L;

    private final Players local;
    private final NetworkRuntimeGateway gateway;
    private final Optional<NetworkInfoService> networkInfo;
    private final Map<UUID, RemotePlayer> remoteCache = new ConcurrentHashMap<>();
    private final String localServerName;

    public NetworkedPlayers(@NotNull Players local, @NotNull NetworkRuntimeGateway gateway,
                             @NotNull Optional<NetworkInfoService> networkInfo) {
        this.local = Objects.requireNonNull(local, "local");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.networkInfo = Objects.requireNonNull(networkInfo, "networkInfo");
        this.localServerName = gateway.runtime().localName();
    }

    public @NotNull Players local() {
        return local;
    }

    @Override
    public @NotNull Collection<RPlayer> online() {
        return local.online();
    }

    @Override
    public @NotNull Optional<RPlayer> get(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        Optional<RPlayer> localPlayer = local.get(uuid);
        if (localPlayer.isPresent()) {
            return localPlayer;
        }

        RemotePlayer cached = remoteCache.get(uuid);
        if (cached != null) {
            return Optional.of(cached);
        }

        return resolveRemotely(uuid);
    }

    @Override
    public @NotNull Optional<RPlayer> wrap(@NotNull Object nativePlayer) {
        return local.wrap(nativePlayer);
    }

    private Optional<RPlayer> resolveRemotely(UUID uuid) {
        if (networkInfo.isEmpty()) {
            return Optional.empty();
        }

        try {
            List<NetworkPlayerInfo> allPlayers = networkInfo.get().players()
                .get(RESOLVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            for (NetworkPlayerInfo info : allPlayers) {
                if (info.uuid().equals(uuid)) {
                    if (info.serverName().equalsIgnoreCase(localServerName)) {
                        return Optional.empty();
                    }
                    RemotePlayer remote = new RemotePlayer(
                        info.uuid(), info.name(), info.serverName(), gateway);
                    remoteCache.put(uuid, remote);
                    return Optional.of(remote);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            logger.debug("Failed to resolve player {} remotely: {}", uuid, e.getMessage());
        }
        return Optional.empty();
    }
}
