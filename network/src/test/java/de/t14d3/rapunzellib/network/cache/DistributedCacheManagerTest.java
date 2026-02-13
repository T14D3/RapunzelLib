package de.t14d3.rapunzellib.network.cache;

import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.testutil.TestNetworkSupport.TestNetwork;
import de.t14d3.rapunzellib.network.testutil.TestNetworkSupport.TestScheduler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class DistributedCacheManagerTest {
    @Test
    void receivesInvalidationsOverGatewayTopic() {
        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();
        AtomicReference<CacheInvalidationMessage> received = new AtomicReference<>();

        try (
            DefaultNetworkRuntimeGateway alphaGateway = network.createGateway("alpha", "proxy", scheduler);
            DefaultNetworkRuntimeGateway betaGateway = network.createGateway("beta", "proxy", scheduler);
            DistributedCacheManager alpha = new DistributedCacheManager(alphaGateway);
            DistributedCacheManager beta = new DistributedCacheManager(betaGateway)
        ) {
            beta.registerInvalidationListener("*", received::set);

            alpha.broadcastInvalidation("example.Entity", "42", InvalidationOperation.DELETE);

            CacheInvalidationMessage message = received.get();
            assertNotNull(message);
            assertEquals("example.Entity", message.entityClassName());
            assertEquals("42", message.id());
            assertEquals(InvalidationOperation.DELETE, message.operation());
            assertEquals("alpha", message.serverName());
        }
    }
}
