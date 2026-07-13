package de.t14d3.rapunzellib.network.remote;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.remote.handler.EntityRpcHandler;
import de.t14d3.rapunzellib.network.remote.handler.PlayerRpcHandler;
import de.t14d3.rapunzellib.network.remote.resolution.NetworkedEntities;
import de.t14d3.rapunzellib.network.remote.resolution.NetworkedPlayers;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.objects.Entities;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public final class RemoteHandlerRegistrar {
    private static final Logger logger = LoggerFactory.getLogger(RemoteHandlerRegistrar.class);

    private RemoteHandlerRegistrar() {}

    public static void install(@NotNull RapunzelContext context, @NotNull NetworkRuntimeGateway gateway) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(gateway, "gateway");

        PlayerRpcHandler playerHandler = new PlayerRpcHandler();
        EntityRpcHandler entityHandler = new EntityRpcHandler();

        playerHandler.register(gateway);
        entityHandler.register(gateway);

        if (context.runtime().hasCapability(RuntimeCapability.ENTITIES)) {
            NetworkInfoService networkInfo = context.services().find(NetworkInfoService.class).orElse(null);

            Players localPlayers = context.players();
            NetworkedPlayers networkedPlayers = new NetworkedPlayers(
                localPlayers, gateway, Optional.ofNullable(networkInfo));
            context.registerIfAbsent(Players.class, networkedPlayers);
            logger.info("[Remote] Replaced Players with NetworkedPlayers (local={}, gateway={})",
                localPlayers.getClass().getSimpleName(), gateway.runtime().localName());

            Entities localEntities = context.entities();
            NetworkedEntities networkedEntities = new NetworkedEntities(
                localEntities, gateway, Optional.ofNullable(networkInfo));
            context.registerIfAbsent(Entities.class, networkedEntities);
            logger.info("[Remote] Replaced Entities with NetworkedEntities");
        }

        logger.info("[Remote] Cross-server RPC handlers installed");
    }
}
