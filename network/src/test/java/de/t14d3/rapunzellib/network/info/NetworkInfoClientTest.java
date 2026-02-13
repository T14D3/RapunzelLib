package de.t14d3.rapunzellib.network.info;

import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.testutil.TestNetworkSupport.TestNetwork;
import de.t14d3.rapunzellib.network.testutil.TestNetworkSupport.TestScheduler;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NetworkInfoClientTest {
    @Test
    void resolvesNetworkInfoViaRuntimeGatewayMethods() {
        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();
        AtomicInteger whoAmICalls = new AtomicInteger();
        NetworkPlayerInfo player = new NetworkPlayerInfo(UUID.randomUUID(), "Alice", "lobby");

        try (
            DefaultNetworkRuntimeGateway proxy = network.createGateway("proxy", "proxy", scheduler);
            DefaultNetworkRuntimeGateway backend = network.createGateway("backend-1", "proxy", scheduler);
            NetworkRuntimeGateway.Subscription whoAmISubscription = proxy.register(
                NetworkInfoRpc.WHO_AM_I_METHOD,
                NetworkRuntimeGateway.RpcHandler.sync((_ignored, sourceServer) -> {
                    whoAmICalls.incrementAndGet();
                    return sourceServer;
                })
            );
            NetworkRuntimeGateway.Subscription serversSubscription = proxy.register(
                NetworkInfoRpc.LIST_SERVERS_METHOD,
                NetworkRuntimeGateway.RpcHandler.sync((_ignored, sourceServer) -> List.of("lobby", "survival"))
            );
            NetworkRuntimeGateway.Subscription playersSubscription = proxy.register(
                NetworkInfoRpc.LIST_PLAYERS_METHOD,
                NetworkRuntimeGateway.RpcHandler.sync((_ignored, sourceServer) -> List.of(player))
            );
            NetworkInfoClient client = new NetworkInfoClient(backend, scheduler, LoggerFactory.getLogger(NetworkInfoClientTest.class))
        ) {
            assertEquals("backend-1", client.networkServerName().join());
            assertEquals("backend-1", client.networkServerName().join());
            assertEquals(1, whoAmICalls.get());
            assertEquals(List.of("lobby", "survival"), client.servers().join());
            assertEquals(List.of(player), client.players().join());
        }
    }
}
