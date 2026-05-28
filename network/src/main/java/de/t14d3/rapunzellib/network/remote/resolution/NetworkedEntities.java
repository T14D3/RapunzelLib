package de.t14d3.rapunzellib.network.remote.resolution;

import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.remote.proxy.RemoteEntity;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.objects.Entities;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NetworkedEntities implements Entities {
    private static final Logger logger = LoggerFactory.getLogger(NetworkedEntities.class);
    private static final long RESOLVE_TIMEOUT_MS = 3000L;

    private final Entities local;
    private final NetworkRuntimeGateway gateway;
    private final Optional<NetworkInfoService> networkInfo;
    private final Map<UUID, RemoteEntity> remoteCache = new ConcurrentHashMap<>();
    private final String localServerName;

    public NetworkedEntities(@NotNull Entities local, @NotNull NetworkRuntimeGateway gateway,
                              @NotNull Optional<NetworkInfoService> networkInfo) {
        this.local = Objects.requireNonNull(local, "local");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.networkInfo = Objects.requireNonNull(networkInfo, "networkInfo");
        this.localServerName = gateway.runtime().localName();
    }

    public @NotNull Entities local() {
        return local;
    }

    @Override
    public @NotNull Optional<REntity> get(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        Optional<REntity> localEntity = local.get(uuid);
        if (localEntity.isPresent()) {
            return localEntity;
        }

        RemoteEntity cached = remoteCache.get(uuid);
        if (cached != null) {
            return Optional.of(cached);
        }

        return resolveRemotely(uuid);
    }

    @Override
    public @NotNull Optional<REntity> wrap(@NotNull Object nativeEntity) {
        return local.wrap(nativeEntity);
    }

    private Optional<REntity> resolveRemotely(UUID uuid) {
        if (networkInfo.isEmpty()) {
            return Optional.empty();
        }

        try {
            List<de.t14d3.rapunzellib.network.info.NetworkPlayerInfo> allPlayers = networkInfo.get().players()
                .get(RESOLVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            for (var info : allPlayers) {
                if (info.uuid().equals(uuid)) {
                    if (info.serverName().equalsIgnoreCase(localServerName)) {
                        return Optional.empty();
                    }
                    RemoteEntity remote = new RemoteEntity(
                        info.uuid(), info.serverName(), gateway,
                        REntityType.ref("minecraft:player"), true);
                    remoteCache.put(uuid, remote);
                    return Optional.of(remote);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            logger.debug("Failed to resolve entity {} remotely: {}", uuid, e.getMessage());
        }
        return Optional.empty();
    }
}
