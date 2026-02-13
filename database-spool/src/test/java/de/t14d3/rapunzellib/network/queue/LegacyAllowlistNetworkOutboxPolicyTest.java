package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.network.NetworkEnvelope;
import de.t14d3.rapunzellib.network.cache.DistributedCacheManager;
import de.t14d3.rapunzellib.network.outbox.NetworkDeliverySemantics;
import de.t14d3.rapunzellib.network.outbox.NetworkSendRequest;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LegacyAllowlistNetworkOutboxPolicyTest {
    @Test
    void allowlistedChannelsUseStoreAndForward() {
        LegacyAllowlistNetworkOutboxPolicy policy = new LegacyAllowlistNetworkOutboxPolicy(Set.of("filesync"));

        assertEquals(
            NetworkDeliverySemantics.STORE_AND_FORWARD,
            policy.semantics(new NetworkSendRequest(NetworkEnvelope.Target.SERVER, "backend-1", "filesync", "payload"))
        );
    }

    @Test
    void nonAllowlistedChannelsStayDirectOnly() {
        LegacyAllowlistNetworkOutboxPolicy policy = new LegacyAllowlistNetworkOutboxPolicy(Set.of("filesync"));

        assertEquals(
            NetworkDeliverySemantics.DIRECT_ONLY,
            policy.semantics(new NetworkSendRequest(NetworkEnvelope.Target.PROXY, null, "rpc:invoke", "payload"))
        );
    }

    @Test
    void defaultAllowlistIncludesDistributedCacheInvalidation() {
        assertTrue(LegacyAllowlistNetworkOutboxPolicy.DEFAULT_CHANNEL_ALLOWLIST.contains(
            DistributedCacheManager.CACHE_INVALIDATION_CHANNEL
        ));
    }
}
